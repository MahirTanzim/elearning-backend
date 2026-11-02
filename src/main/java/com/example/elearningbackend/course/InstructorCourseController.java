package com.example.elearningbackend.course;

import com.example.elearningbackend.course.CourseDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService service;

    // ----- courses -----

    @GetMapping("/courses")
    public List<CourseSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(userId(jwt));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseRequest request) {
        return service.create(userId(jwt), request);
    }

    @GetMapping("/courses/{id}")
    public CourseDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getMine(userId(jwt), id);
    }

    @PutMapping("/courses/{id}")
    public CourseDetail update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody CourseRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(userId(jwt), id);
    }

    // ----- sections -----

    @PostMapping("/courses/{id}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetail addSection(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody SectionRequest request) {
        return service.addSection(userId(jwt), id, request);
    }

    @PutMapping("/courses/{id}/sections/order")
    public CourseDetail reorderSections(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @Valid @RequestBody OrderRequest request) {
        return service.reorderSections(userId(jwt), id, request);
    }

    @PutMapping("/sections/{sectionId}")
    public CourseDetail renameSection(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sectionId,
                                      @Valid @RequestBody SectionRequest request) {
        return service.renameSection(userId(jwt), sectionId, request);
    }

    @DeleteMapping("/sections/{sectionId}")
    public CourseDetail deleteSection(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sectionId) {
        return service.deleteSection(userId(jwt), sectionId);
    }

    // ----- lessons -----

    @PostMapping("/sections/{sectionId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetail addLesson(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sectionId,
                                  @Valid @RequestBody LessonCreateRequest request) {
        return service.addLesson(userId(jwt), sectionId, request);
    }

    @PutMapping("/sections/{sectionId}/lessons/order")
    public CourseDetail reorderLessons(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sectionId,
                                       @Valid @RequestBody OrderRequest request) {
        return service.reorderLessons(userId(jwt), sectionId, request);
    }

    @PutMapping("/lessons/{lessonId}")
    public CourseDetail updateLesson(@AuthenticationPrincipal Jwt jwt, @PathVariable Long lessonId,
                                     @Valid @RequestBody LessonUpdateRequest request) {
        return service.updateLesson(userId(jwt), lessonId, request);
    }

    @DeleteMapping("/lessons/{lessonId}")
    public CourseDetail deleteLesson(@AuthenticationPrincipal Jwt jwt, @PathVariable Long lessonId) {
        return service.deleteLesson(userId(jwt), lessonId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}