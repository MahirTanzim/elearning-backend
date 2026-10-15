package com.example.elearningbackend.user;

import java.util.Set;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    String status,
    Set<String> roles
) {}