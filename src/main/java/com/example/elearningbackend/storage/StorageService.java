package com.example.elearningbackend.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface StorageService {

    /** Saves the bytes in the given folder and returns the key used to find them again. */
    String store(String folder, byte[] data, String extension);

    /** Same, but copies a stream to disk without loading it all into memory (for videos). */
    String store(String folder, InputStream data, String extension);

    /** Old two-argument form, still used by certificate uploads: goes to the "documents" folder. */
    default String store(byte[] data, String extension) {
        return store("documents", data, extension);
    }

    Resource load(String key);

    void delete(String key);
}