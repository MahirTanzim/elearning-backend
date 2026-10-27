package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.ApplicationDtos.ApplicationResponse;
import com.example.elearningbackend.storage.StorageCleanup;
import com.example.elearningbackend.storage.StorageService;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QualificationDocumentService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PER_QUALIFICATION = 3;
    private static final Set<String> EDITABLE = Set.of(InstructorProfile.DRAFT, InstructorProfile.REJECTED);

    private enum FileType {
        PDF("application/pdf", "pdf"),
        PNG("image/png", "png"),
        JPEG("image/jpeg", "jpg");

        final String contentType;
        final String extension;

        FileType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }
    }

    public record Download(Resource resource, String fileName, String contentType) {}

    private final InstructorProfileRepository profiles;
    private final QualificationDocumentRepository documents;
    private final StorageService storage;
    private final StorageCleanup cleanup;

    @Transactional
    public ApplicationResponse upload(Long userId, Long qualificationId, MultipartFile file) {
        InstructorProfile profile = myProfile(userId);
        requireEditable(profile);

        Qualification qualification = profile.getQualifications().stream()
            .filter(q -> q.getId().equals(qualificationId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualification not found"));

        if (qualification.getDocuments().size() >= MAX_PER_QUALIFICATION) {
            throw bad("You can upload at most " + MAX_PER_QUALIFICATION + " files per qualification");
        }
        if (file.isEmpty()) {
            throw bad("The file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw bad("File is too large (max 5 MB)");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the file");
        }

        FileType type = detect(data);
        if (type == null) {
            throw bad("Only PDF, PNG and JPG files are allowed");
        }

        String key = storage.store(data, type.extension);
        cleanup.deleteIfRolledBack(key);

        QualificationDocument doc = new QualificationDocument();
        doc.setQualification(qualification);
        doc.setStorageKey(key);
        doc.setOriginalName(safeName(file.getOriginalFilename(), type));
        doc.setContentType(type.contentType);   // our detected type, never the client's claim
        doc.setSizeBytes(data.length);
        qualification.getDocuments().add(doc);

        return ApplicationResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public ApplicationResponse delete(Long userId, Long documentId) {
        InstructorProfile profile = myProfile(userId);
        requireEditable(profile);

        for (Qualification q : profile.getQualifications()) {
            for (QualificationDocument d : q.getDocuments()) {
                if (d.getId().equals(documentId)) {
                    cleanup.deleteAfterCommit(d.getStorageKey());
                    q.getDocuments().remove(d);
                    return ApplicationResponse.from(profiles.saveAndFlush(profile));
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, boolean isAdmin, Long documentId) {
        QualificationDocument doc = documents.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        Long ownerId = doc.getQualification().getInstructor().getUser().getId();
        if (!isAdmin && !ownerId.equals(userId)) {
            // 404, not 403: do not even reveal that this document exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return new Download(storage.load(doc.getStorageKey()), doc.getOriginalName(), doc.getContentType());
    }

    // ---------- helpers ----------

    private InstructorProfile myProfile(Long userId) {
        return profiles.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No application yet"));
    }

    private void requireEditable(InstructorProfile profile) {
        if (!EDITABLE.contains(profile.getApplicationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Documents cannot be changed while the application is " + profile.getApplicationStatus());
        }
    }

    /** Looks at the first bytes of the file, not at its name or the declared type. */
    private FileType detect(byte[] d) {
        if (startsWith(d, 0x25, 0x50, 0x44, 0x46, 0x2D)) return FileType.PDF;                    // %PDF-
        if (startsWith(d, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return FileType.PNG;
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) return FileType.JPEG;
        return null;
    }

    private boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    /** The name is only for display and download, but we still clean it. */
    private String safeName(String original, FileType type) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.length() > 100) name = name.substring(name.length() - 100);
        return name.isBlank() ? "document." + type.extension : name;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}