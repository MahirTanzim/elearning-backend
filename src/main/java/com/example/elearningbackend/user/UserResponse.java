package com.example.elearningbackend.user;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    String status,
    boolean emailVerified,
    Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getStatus(),
            user.isEmailVerified(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
    }
}