package com.example.elearningbackend.auth;

import com.example.elearningbackend.security.JwtService;
import com.example.elearningbackend.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role learnerRole = roleRepository.findByName("LEARNER")
                .orElseThrow(() -> new IllegalStateException("LEARNER role missing"));

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider("LOCAL");
        user.getRoles().add(learnerRole);

        User saved = userRepository.save(user);
        accountService.sendVerificationEmail(saved);
        return UserResponse.from(saved);
    }

    private final RefreshTokenService refreshTokenService; // add next to the other fields

    @Transactional // was readOnly = true: login now writes a refresh token
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        return buildAuthResponse(user, refreshTokenService.create(user));
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(request.refreshToken());
        return buildAuthResponse(rotated.user(), rotated.newRefreshToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.getExpiresInSeconds(),
                UserResponse.from(user));
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}