package com.example.elearningbackend.course;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class CourseDtos {

    private CourseDtos() {}

    // ---------- requests ----------

    public record CourseRequest(
        @NotBlank @Size(min = 5, max = 120, message = "Title must be 5-120 characters") String title,
        @Size(max = 200) String subtitle,
        @Size(max = 5000) String description,
        @NotNull Long categoryId,
        @NotBlank @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|ALL_LEVELS", message = "Unknown level") String level,
        @NotBlank @Size(max = 30) String language,
        @NotNull @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "9999.99", message = "Price is too high")
        @Digits(integer = 4, fraction = 2, message = "Price can have at most 2 decimals") BigDecimal price,
        @Size(max = 2000) String whatYouLearn,
        @Size(max = 2000) String requirements
    ) {}

    public record SectionRequest(@NotBlank @Size(max = 150) String title) {}

    public record LessonCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Pattern(regexp = "VIDEO|PDF|TEXT", message = "Type must be VIDEO, PDF or TEXT") String type,
        boolean freePreview,
        @Size(max = 2000) String description,
        @Size(max = 20000) String textContent
    ) {}

    public record LessonUpdateRequest(
        @NotBlank @Size(max = 150) String title,
        boolean freePreview,
        @Size(max = 2000) String description,
        @Size(max = 20000) String textContent
    ) {}

    public record OrderRequest(@NotNull @Size(max = 200) List<Long> ids) {}

    // ---------- responses ----------

    public record CategoryResponse(Long id, String name, String slug) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
        }
    }

    public record CourseSummary(
        Long id, String title, String status, BigDecimal price, String currency,
        String categoryName, LocalDateTime updatedAt
    ) {
        public static CourseSummary from(Course c) {
            return new CourseSummary(c.getId(), c.getTitle(), c.getStatus(), c.getPrice(),
                c.getCurrency(), c.getCategory().getName(), c.getUpdatedAt());
        }
    }

    public record LessonResponse(
        Long id, String title, String type, int position, boolean freePreview,
        String description, String textContent, Integer durationSeconds
    ) {
        public static LessonResponse from(Lesson l) {
            return new LessonResponse(l.getId(), l.getTitle(), l.getType(), l.getPosition(),
                l.isFreePreview(), l.getDescription(), l.getTextContent(), l.getDurationSeconds());
        }
    }

    public record SectionResponse(Long id, String title, int position, List<LessonResponse> lessons) {
        public static SectionResponse from(Section s) {
            return new SectionResponse(s.getId(), s.getTitle(), s.getPosition(),
                s.getLessons().stream()
                    .sorted(Comparator.comparingInt(Lesson::getPosition))
                    .map(LessonResponse::from)
                    .toList());
        }
    }

    public record CourseDetail(
        Long id, String title, String slug, String subtitle, String description,
        Long categoryId, String categoryName, String level, String language,
        BigDecimal price, String currency, String whatYouLearn, String requirements,
        String status, String rejectionReason, List<SectionResponse> sections
    ) {
        public static CourseDetail from(Course c) {
            return new CourseDetail(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(), c.getDescription(),
                c.getCategory().getId(), c.getCategory().getName(), c.getLevel(), c.getLanguage(),
                c.getPrice(), c.getCurrency(), c.getWhatYouLearn(), c.getRequirements(),
                c.getStatus(), c.getRejectionReason(),
                c.getSections().stream()
                    .sorted(Comparator.comparingInt(Section::getPosition))
                    .map(SectionResponse::from)
                    .toList()
            );
        }
    }
}