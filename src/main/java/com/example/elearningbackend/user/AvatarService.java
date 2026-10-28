package com.example.elearningbackend.user;

import com.example.elearningbackend.storage.StorageCleanup;
import com.example.elearningbackend.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4000;

    private final UserRepository users;
    private final StorageService storage;
    private final StorageCleanup cleanup;

    @Transactional
    public UserResponse upload(Long userId, MultipartFile file) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (file.isEmpty()) throw bad("The file is empty");
        if (file.getSize() > MAX_BYTES) throw bad("Image is too large (max 2 MB)");

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the file");
        }

        String extension = detectExtension(data);
        if (extension == null) throw bad("Only PNG and JPG images are allowed");
        checkDimensions(data);

        String key = storage.store("avatars", data, extension);
        cleanup.deleteIfRolledBack(key);

        String oldKey = user.getAvatarKey();
        user.setAvatarKey(key);
        if (oldKey != null) cleanup.deleteAfterCommit(oldKey);

        return UserResponse.from(users.saveAndFlush(user));
    }

    @Transactional
    public UserResponse remove(Long userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String oldKey = user.getAvatarKey();
        if (oldKey != null) {
            user.setAvatarKey(null);
            cleanup.deleteAfterCommit(oldKey);
        }
        return UserResponse.from(users.saveAndFlush(user));
    }

    // ---------- helpers ----------

    /** Looks at the first bytes, not at the file name or the type the browser claims. */
    private String detectExtension(byte[] d) {
        if (startsWith(d, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) return "jpg";
        return null;
    }

    private boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    /** Reads only the image header (width and height), never the pixels. */
    private void checkDimensions(byte[] data) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in == null) throw bad("Could not read the image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw bad("Could not read the image");

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw bad("Image dimensions must be at most " + MAX_DIMENSION + "x" + MAX_DIMENSION + " pixels");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw bad("Could not read the image");
        }
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}