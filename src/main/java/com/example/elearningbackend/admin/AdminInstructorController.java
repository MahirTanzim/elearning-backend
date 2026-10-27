package com.example.elearningbackend.admin;

import com.example.elearningbackend.admin.AdminDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminInstructorController {

    private final AdminInstructorService service;

    @GetMapping("/instructor-applications")
    public List<ApplicantSummary> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @GetMapping("/instructor-applications/{id}")
    public ApplicationDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/qualifications/{id}/verified")
    public ApplicationDetail verify(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long id,
                                    @Valid @RequestBody VerifyRequest request) {
        return service.setQualificationVerified(adminId(jwt), id, request.verified());
    }

    @PostMapping("/instructor-applications/{id}/approve")
    public ApplicationDetail approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.approve(adminId(jwt), id);
    }

    @PostMapping("/instructor-applications/{id}/reject")
    public ApplicationDetail reject(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long id,
                                    @Valid @RequestBody RejectRequest request) {
        return service.reject(adminId(jwt), id, request.reason());
    }

    private Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}