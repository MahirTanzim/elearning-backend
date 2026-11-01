package com.example.elearningbackend.instructor;

import java.util.List;

public final class PublicInstructorDtos {

    private PublicInstructorDtos() {}

    public record PublicQualification(
        String type, String title, String institution, String fieldOfStudy, Integer startYear, Integer endYear
    ) {}

    public record PublicSocialLink(String platform, String url) {}

    public record InstructorCard(Long id, String fullName, String avatarUrl, String headline, String expertise) {}

    public record InstructorPage(
        Long id,
        String fullName,
        String avatarUrl,
        String headline,
        String bio,
        String expertise,
        List<PublicQualification> qualifications,
        List<PublicSocialLink> socialLinks
    ) {}
}