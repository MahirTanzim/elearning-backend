package com.example.elearningbackend.instructor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    Optional<InstructorProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<InstructorProfile> findByApplicationStatusOrderBySubmittedAtAsc(String status);

    // public pages: approved profiles whose account is also active (banned people disappear)
    @EntityGraph(attributePaths = "user")
    List<InstructorProfile> findTop50ByApplicationStatusAndUserStatusOrderByReviewedAtDesc(
        String applicationStatus, String userStatus);

    @EntityGraph(attributePaths = "user")
    Optional<InstructorProfile> findByIdAndApplicationStatusAndUserStatus(
        Long id, String applicationStatus, String userStatus);
}