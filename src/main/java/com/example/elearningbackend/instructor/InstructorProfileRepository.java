package com.example.elearningbackend.instructor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    Optional<InstructorProfile> findByUserId(Long userId);

    // loads the user in the same query, avoiding one extra query per row
    @EntityGraph(attributePaths = "user")
    List<InstructorProfile> findByApplicationStatusOrderBySubmittedAtAsc(String status);
}
