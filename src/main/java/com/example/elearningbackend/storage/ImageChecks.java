package com.example.elearningbackend.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ImageChecks {

    /** Looks at the first bytes. Returns "png", "jpg" or null. */
    public String detectExtension(byte[] d) {
        if (startsWith(d, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) return "jpg";
        return null;
    }

    /** Reads only the image header (width and height), never the pixels. */
    public void requireDimensions(byte[] data, int minW, int minH, int maxW, int maxH) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in == null) throw bad("Could not read the image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw bad("Could not read the image");

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < minW || height < minH) {
                    throw bad("Image is too small (at least " + minW + "x" + minH + " pixels)");
                }
                if (width > maxW || height > maxH) {
                    throw bad("Image is too large (at most " + maxW + "x" + maxH + " pixels)");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw bad("Could not read the image");
        }
    }

    private boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}