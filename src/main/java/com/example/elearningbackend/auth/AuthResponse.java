package com.example.elearningbackend.auth;

import com.example.elearningbackend.user.UserResponse;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {}