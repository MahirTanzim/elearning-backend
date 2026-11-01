package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.PublicInstructorDtos.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/instructors")
@RequiredArgsConstructor
public class PublicInstructorController {

    private final PublicInstructorService service;

    @GetMapping
    public List<InstructorCard> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public InstructorPage get(@PathVariable Long id) {
        return service.get(id);
    }
}