package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.ApplicationDtos.*;
import com.example.elearningbackend.storage.StorageCleanup;
import com.example.elearningbackend.user.User;
import com.example.elearningbackend.user.UserRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InstructorApplicationService {

    private static final Set<String> PROFILE_EDITABLE = Set.of(
            InstructorProfile.DRAFT, InstructorProfile.REJECTED, InstructorProfile.APPROVED);
    private static final Set<String> QUALIFICATIONS_EDITABLE = Set.of(
            InstructorProfile.DRAFT, InstructorProfile.REJECTED);
    private static final int MAX_QUALIFICATIONS = 10;

    private final InstructorProfileRepository profiles;
    private final UserRepository users;
    private final StorageCleanup cleanup;

    @Transactional(readOnly = true)
    public ApplicationResponse getMine(Long userId) {
        return profiles.findByUserId(userId)
                .map(ApplicationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No application yet"));
    }

    @Transactional
    public ApplicationResponse saveProfile(Long userId, ProfileRequest request) {
        InstructorProfile p = getOrCreate(userId);
        requireEditable(p, PROFILE_EDITABLE);

        p.setHeadline(clean(request.headline()));
        p.setBio(clean(request.bio()));
        p.setExpertise(clean(request.expertise()));
        return ApplicationResponse.from(profiles.saveAndFlush(p));
    }

    @Transactional
    public ApplicationResponse addQualification(Long userId, QualificationRequest request) {
        InstructorProfile p = getOrCreate(userId);
        requireEditable(p, QUALIFICATIONS_EDITABLE);

        if (p.getQualifications().size() >= MAX_QUALIFICATIONS) {
            throw bad("You can add at most " + MAX_QUALIFICATIONS + " qualifications");
        }
        if (request.startYear() != null && request.endYear() != null
                && request.endYear() < request.startYear()) {
            throw bad("End year cannot be before start year");
        }

        Qualification q = new Qualification();
        q.setInstructor(p);
        q.setTitle(request.title().trim());
        q.setInstitution(request.institution().trim());
        q.setFieldOfStudy(clean(request.fieldOfStudy()));
        q.setStartYear(request.startYear());
        q.setEndYear(request.endYear());
        q.setType(request.type());
        p.getQualifications().add(q);

        // saveAndFlush so the new row gets its id before we build the response
        return ApplicationResponse.from(profiles.saveAndFlush(p));
    }

    @Transactional
    public ApplicationResponse deleteQualification(Long userId, Long qualificationId) {
        InstructorProfile p = getOrCreate(userId);
        requireEditable(p, QUALIFICATIONS_EDITABLE);
        Qualification target = p.getQualifications().stream()
                .filter(q -> q.getId().equals(qualificationId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualification not found"));
        // we only search inside THIS user's own list, so other people's ids are simply
        // "not found"
        target.getDocuments().forEach(d -> cleanup.deleteAfterCommit(d.getStorageKey()));
        p.getQualifications().remove(target);
        return ApplicationResponse.from(profiles.saveAndFlush(p));
    }

    @Transactional
    public ApplicationResponse replaceSocialLinks(Long userId, SocialLinksRequest request) {
        InstructorProfile p = getOrCreate(userId);
        requireEditable(p, PROFILE_EDITABLE);

        Set<String> seen = new HashSet<>();
        List<SocialLink> newLinks = request.links().stream().map(r -> {
            if (!seen.add(r.platform())) {
                throw bad("Only one link per platform (duplicate: " + r.platform() + ")");
            }
            SocialLink link = new SocialLink();
            link.setInstructor(p);
            link.setPlatform(r.platform());
            link.setUrl(validUrl(r.url()));
            return link;
        }).toList();

        p.getSocialLinks().clear();
        p.getSocialLinks().addAll(newLinks);
        return ApplicationResponse.from(profiles.saveAndFlush(p));
    }

    @Transactional
    public ApplicationResponse submit(Long userId) {
        InstructorProfile p = profiles.findByUserId(userId)
                .orElseThrow(() -> bad("Fill in your profile first"));

        String status = p.getApplicationStatus();
        if (InstructorProfile.PENDING.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your application is already under review");
        }
        if (!QUALIFICATIONS_EDITABLE.contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application cannot be submitted");
        }
        if (!p.getUser().isEmailVerified()) {
            throw bad("Please verify your email address first");
        }
        if (p.getHeadline() == null || p.getBio() == null) {
            throw bad("Headline and bio are required");
        }
        if (p.getQualifications().isEmpty()) {
            throw bad("Add at least one qualification");
        }

        p.setApplicationStatus(InstructorProfile.PENDING);
        p.setSubmittedAt(LocalDateTime.now());
        p.setRejectionReason(null);
        return ApplicationResponse.from(p);
    }

    // ---------- helpers ----------

    private InstructorProfile getOrCreate(Long userId) {
        return profiles.findByUserId(userId).orElseGet(() -> {
            User user = users.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            InstructorProfile p = new InstructorProfile();
            p.setUser(user);
            return profiles.save(p);
        });
    }

    private void requireEditable(InstructorProfile p, Set<String> allowedStatuses) {
        if (!allowedStatuses.contains(p.getApplicationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This cannot be changed while the application is " + p.getApplicationStatus());
        }
    }

    private String validUrl(String raw) {
        String url = raw.trim();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            boolean ok = uri.getHost() != null && scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!ok)
                throw bad("Links must start with http:// or https://");
            return url;
        } catch (URISyntaxException e) {
            throw bad("Invalid link: " + url);
        }
    }

    private String clean(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}