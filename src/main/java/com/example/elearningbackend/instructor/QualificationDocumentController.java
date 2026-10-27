package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.ApplicationDtos.ApplicationResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class QualificationDocumentController {

    private final QualificationDocumentService service;

    @PostMapping(value = "/api/instructor-application/qualifications/{id}/documents",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse upload(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long id,
                                      @RequestParam("file") MultipartFile file) {
        return service.upload(Long.valueOf(jwt.getSubject()), id, file);
    }

    @DeleteMapping("/api/instructor-application/documents/{documentId}")
    public ApplicationResponse delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long documentId) {
        return service.delete(Long.valueOf(jwt.getSubject()), documentId);
    }

    /** The only way to read a document: owner or admin. */
    @GetMapping("/api/documents/{documentId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt,
                                             Authentication authentication,
                                             @PathVariable Long documentId) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        QualificationDocumentService.Download file =
            service.download(Long.valueOf(jwt.getSubject()), isAdmin, documentId);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(file.resource());
    }
}