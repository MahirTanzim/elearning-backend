package com.example.elearningbackend.admin;

import com.example.elearningbackend.admin.AdminDtos.*;
import com.example.elearningbackend.auth.EmailService;
import com.example.elearningbackend.instructor.InstructorProfile;
import com.example.elearningbackend.instructor.InstructorProfileRepository;
import com.example.elearningbackend.instructor.Qualification;
import com.example.elearningbackend.instructor.QualificationRepository;
import com.example.elearningbackend.user.Role;
import com.example.elearningbackend.user.RoleRepository;
import com.example.elearningbackend.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInstructorService {

    private static final Set<String> LISTABLE = Set.of(
        InstructorProfile.PENDING, InstructorProfile.APPROVED,
        InstructorProfile.REJECTED, InstructorProfile.SUSPENDED);

    private final InstructorProfileRepository profiles;
    private final QualificationRepository qualifications;
    private final RoleRepository roles;
    private final AuditService audit;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<ApplicantSummary> list(String status) {
        String wanted = (status == null || status.isBlank())
            ? InstructorProfile.PENDING : status.trim().toUpperCase();
        if (!LISTABLE.contains(wanted)) {
            throw bad("Unknown status");
        }
        return profiles.findByApplicationStatusOrderBySubmittedAtAsc(wanted).stream()
            .map(p -> new ApplicantSummary(
                p.getId(), p.getUser().getId(), p.getUser().getFullName(), p.getUser().getEmail(),
                p.getHeadline(), p.getApplicationStatus(), p.getSubmittedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDetail get(Long profileId) {
        return ApplicationDetail.from(findReviewable(profileId));
    }

    @Transactional
    public ApplicationDetail setQualificationVerified(Long adminId, Long qualificationId, boolean verified) {
        Qualification q = qualifications.findById(qualificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualification not found"));
        InstructorProfile p = q.getInstructor();

        requirePending(p);
        requireNotSelf(adminId, p);

        q.setVerified(verified);
        audit.log(adminId, verified ? "VERIFY_QUALIFICATION" : "UNVERIFY_QUALIFICATION",
            "QUALIFICATION", q.getId(), q.getTitle() + " (" + q.getInstitution() + ")");
        return ApplicationDetail.from(p);
    }

    @Transactional
    public ApplicationDetail approve(Long adminId, Long profileId) {
        InstructorProfile p = findReviewable(profileId);
        requirePending(p);
        requireNotSelf(adminId, p);

        if (p.getQualifications().stream().noneMatch(Qualification::isVerified)) {
            throw bad("Verify at least one qualification before approving");
        }

        User user = p.getUser();
        Role instructorRole = roles.findByName("INSTRUCTOR")
            .orElseThrow(() -> new IllegalStateException("INSTRUCTOR role missing"));
        if (user.getRoles().stream().noneMatch(r -> r.getName().equals("INSTRUCTOR"))) {
            user.getRoles().add(instructorRole);
        }

        p.setApplicationStatus(InstructorProfile.APPROVED);
        p.setReviewedAt(LocalDateTime.now());
        p.setReviewedBy(adminId);
        p.setRejectionReason(null);

        audit.log(adminId, "APPROVE_INSTRUCTOR", "INSTRUCTOR_PROFILE", p.getId(), user.getEmail());

        sendAfterCommit(user.getEmail(), "Your instructor application was approved",
            "Hi " + user.getFullName() + ",\n\n"
          + "Good news: your instructor application has been approved.\n"
          + "Open " + frontendUrl + "/become-instructor and press \"Activate instructor access\" "
          + "(or log in again) to start using your instructor account.");

        return ApplicationDetail.from(p);
    }

    @Transactional
    public ApplicationDetail reject(Long adminId, Long profileId, String reason) {
        InstructorProfile p = findReviewable(profileId);
        requirePending(p);
        requireNotSelf(adminId, p);

        String cleanReason = reason.trim();
        p.setApplicationStatus(InstructorProfile.REJECTED);
        p.setReviewedAt(LocalDateTime.now());
        p.setReviewedBy(adminId);
        p.setRejectionReason(cleanReason);

        User user = p.getUser();
        audit.log(adminId, "REJECT_INSTRUCTOR", "INSTRUCTOR_PROFILE", p.getId(),
            user.getEmail() + ": " + cleanReason);

        sendAfterCommit(user.getEmail(), "Your instructor application needs changes",
            "Hi " + user.getFullName() + ",\n\n"
          + "We could not approve your instructor application yet.\n\n"
          + "Reason: " + cleanReason + "\n\n"
          + "You can fix the problems and submit again at " + frontendUrl + "/become-instructor");

        return ApplicationDetail.from(p);
    }

    // ---------- helpers ----------

    private InstructorProfile findReviewable(Long profileId) {
        InstructorProfile p = profiles.findById(profileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (InstructorProfile.DRAFT.equals(p.getApplicationStatus())) {
            // not submitted yet: admins have no business reading it
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        return p;
    }

    private void requirePending(InstructorProfile p) {
        if (!InstructorProfile.PENDING.equals(p.getApplicationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This application is " + p.getApplicationStatus() + ", not waiting for review");
        }
    }

    private void requireNotSelf(Long adminId, InstructorProfile p) {
        if (p.getUser().getId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own application");
        }
    }

    /** The email is sent only if the database change really committed. */
    private void sendAfterCommit(String to, String subject, String body) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.send(to, subject, body);
                } catch (Exception e) {
                    log.error("Could not send email to {}", to, e);
                }
            }
        });
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}