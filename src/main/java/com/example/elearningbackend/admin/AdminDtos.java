package com.example.elearningbackend.admin;

import com.example.elearningbackend.instructor.ApplicationDtos.*;
import com.example.elearningbackend.instructor.InstructorProfile;
import com.example.elearningbackend.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {}

    public record RejectRequest(
        @NotBlank @Size(min = 10, max = 1000, message = "Please give a reason of 10-1000 characters")
        String reason
    ) {}

    public record VerifyRequest(@NotNull Boolean verified) {}

    public record ApplicantSummary(
        Long id, Long userId, String fullName, String email,
        String headline, String status, LocalDateTime submittedAt
    ) {}

    public record ApplicantUser(
        Long id, String fullName, String email, boolean emailVerified, String avatarUrl
    ) {}

    public record ApplicationDetail(
        Long id,
        String status,
        ApplicantUser user,
        String headline,
        String bio,
        String expertise,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        List<QualificationResponse> qualifications,
        List<SocialLinkResponse> socialLinks
    ) {
        public static ApplicationDetail from(InstructorProfile p) {
            ApplicationResponse base = ApplicationResponse.from(p);   // reuse the mapping we already wrote
            User u = p.getUser();
            return new ApplicationDetail(
                p.getId(),
                p.getApplicationStatus(),
                new ApplicantUser(u.getId(), u.getFullName(), u.getEmail(), u.isEmailVerified(), u.getAvatarUrl()),
                p.getHeadline(),
                p.getBio(),
                p.getExpertise(),
                p.getRejectionReason(),
                p.getSubmittedAt(),
                p.getReviewedAt(),
                base.qualifications(),
                base.socialLinks()
            );
        }
    }

    public record AuditLogResponse(
        Long id, Long adminId, String action, String targetType,
        Long targetId, String details, LocalDateTime createdAt
    ) {
        public static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(a.getId(), a.getAdminId(), a.getAction(),
                a.getTargetType(), a.getTargetId(), a.getDetails(), a.getCreatedAt());
        }
    }
}