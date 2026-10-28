package com.example.elearningbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        return avatarService.upload(Long.valueOf(jwt.getSubject()), file);
    }

    @DeleteMapping
    public UserResponse remove(@AuthenticationPrincipal Jwt jwt) {
        return avatarService.remove(Long.valueOf(jwt.getSubject()));
    }
}