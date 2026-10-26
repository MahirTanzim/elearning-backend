package com.example.elearningbackend.instructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ApplicationDtos {

    private ApplicationDtos() {}


    public record ProfileRequest(
        @Size(max = 150) String headline,
        @Size(max = 2000) String bio,
        @Size(max = 255) String expertise
    ) {}

    public record QualificationRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String institution,
        @Size(max = 200) String fieldOfStudy,
        @Min(1950) @Max(2100) Integer startYear,
        @Min(1950) @Max(2100) Integer endYear,
        @NotBlank @Pattern(regexp = "DEGREE|CERTIFICATION|EXPERIENCE",
                           message = "Type must be DEGREE, CERTIFICATION or EXPERIENCE") String type
    ) {}

    public record SocialLinkRequest(
        @NotBlank @Pattern(regexp = "LINKEDIN|GITHUB|FACEBOOK|INSTAGRAM|PINTEREST|X|YOUTUBE|WEBSITE|OTHER",
                           message = "Unknown platform") String platform,
        @NotBlank @Size(max = 500) String url
    ) {}

    public record SocialLinksRequest(
        @NotNull @Valid @Size(max = 10, message = "At most 10 links") List<SocialLinkRequest> links
    ) {}


    public record QualificationResponse(
        Long id, String title, String institution, String fieldOfStudy,
        Integer startYear, Integer endYear, String type, boolean verified
    ) {}

    public record SocialLinkResponse(String platform, String url) {}

    public record ApplicationResponse(
        Long id,
        String status,
        String rejectionReason,
        String headline,
        String bio,
        String expertise,
        LocalDateTime submittedAt,
        List<QualificationResponse> qualifications,
        List<SocialLinkResponse> socialLinks
    ) {
        public static ApplicationResponse from(InstructorProfile p) {
            return new ApplicationResponse(
                p.getId(),
                p.getApplicationStatus(),
                p.getRejectionReason(),
                p.getHeadline(),
                p.getBio(),
                p.getExpertise(),
                p.getSubmittedAt(),
                p.getQualifications().stream()
                    .map(q -> new QualificationResponse(q.getId(), q.getTitle(), q.getInstitution(),
                        q.getFieldOfStudy(), q.getStartYear(), q.getEndYear(), q.getType(), q.isVerified()))
                    .toList(),
                p.getSocialLinks().stream()
                    .map(l -> new SocialLinkResponse(l.getPlatform(), l.getUrl()))
                    .toList()
            );
        }
    }
}