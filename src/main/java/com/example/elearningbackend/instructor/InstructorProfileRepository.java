package com.example.elearningbackend.instructor;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {
    Optional<InstructorProfile> findByUserId(Long userId);
}