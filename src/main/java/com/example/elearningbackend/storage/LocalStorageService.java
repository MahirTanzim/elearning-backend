package com.example.elearningbackend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${app.storage.local-root}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage folder " + root, e);
        }
    }

    @Override
    public String store(String folder, byte[] data, String extension) {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        try {
            Path path = resolve(key);
            Files.createDirectories(path.getParent());
            Files.write(path, data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        return key;
    }
    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", key, e);
        }
    }

    // Defence in depth: even a bad key can never point outside the storage folder.
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}