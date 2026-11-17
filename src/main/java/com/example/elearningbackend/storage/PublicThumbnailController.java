package com.example.elearningbackend.storage;

import java.time.Duration;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class PublicThumbnailController {

    private static final Pattern FILE_NAME = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)$");

    private final StorageService storage;

    @GetMapping("/api/public/thumbnails/{file}")
    public ResponseEntity<Resource> thumbnail(@PathVariable String file) {
        if (!FILE_NAME.matcher(file).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }

        Resource resource = storage.load("thumbnails/" + file);
        MediaType type = file.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
            .contentType(type)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
    }
}