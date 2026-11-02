package com.example.elearningbackend.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = "category")
    List<Course> findByInstructorUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Course> findByIdAndInstructorUserId(Long id, Long userId);

    boolean existsBySlug(String slug);

    long countByInstructorId(Long instructorId);
}