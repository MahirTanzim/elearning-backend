package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.ApplicationDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructor-application")
@RequiredArgsConstructor
public class InstructorApplicationController {

    private final InstructorApplicationService service;

    @GetMapping
    public ApplicationResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.getMine(userId(jwt));
    }

    @PutMapping
    public ApplicationResponse saveProfile(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody ProfileRequest request) {
        return service.saveProfile(userId(jwt), request);
    }

    @PostMapping("/qualifications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse addQualification(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody QualificationRequest request) {
        return service.addQualification(userId(jwt), request);
    }

    @DeleteMapping("/qualifications/{id}")
    public ApplicationResponse deleteQualification(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long id) {
        return service.deleteQualification(userId(jwt), id);
    }

    @PutMapping("/social-links")
    public ApplicationResponse saveLinks(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody SocialLinksRequest request) {
        return service.replaceSocialLinks(userId(jwt), request);
    }

    @PostMapping("/submit")
    public ApplicationResponse submit(@AuthenticationPrincipal Jwt jwt) {
        return service.submit(userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}