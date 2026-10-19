package com.example.elearningbackend.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {}