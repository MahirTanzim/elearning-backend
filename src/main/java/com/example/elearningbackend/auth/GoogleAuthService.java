package com.example.elearningbackend.auth;

import com.example.elearningbackend.security.JwtService;
import com.example.elearningbackend.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    public record Tokens(String accessToken, String refreshToken) {}

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Tokens loginOrRegister(String email, String name, String picture, String googleId) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            Role learnerRole = roleRepository.findByName("LEARNER")
                .orElseThrow(() -> new IllegalStateException("LEARNER role missing"));

            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(name != null && !name.isBlank() ? name : normalizedEmail);
            user.setAvatarUrl(picture);
            user.setAuthProvider("GOOGLE");
            user.setProviderId(googleId);
            user.setEmailVerified(true);
            user.getRoles().add(learnerRole);
            user = userRepository.save(user);
        } else {
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
            }
            // Same email already registered (e.g. with a password): link it to Google.
            if (user.getProviderId() == null) {
                user.setProviderId(googleId);
            }
            user.setEmailVerified(true);
            if (user.getAvatarUrl() == null) {
                user.setAvatarUrl(picture);
            }
        }

        return new Tokens(
            jwtService.generateAccessToken(user),
            refreshTokenService.create(user)
        );
    }
}