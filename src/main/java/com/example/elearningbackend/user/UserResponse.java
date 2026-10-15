package com.example.elearningbackend.user;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    String status,
    Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getStatus(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
    }
}