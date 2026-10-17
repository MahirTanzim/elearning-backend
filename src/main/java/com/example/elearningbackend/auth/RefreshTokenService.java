package com.example.elearningbackend.auth;

import com.example.elearningbackend.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record Rotated(User user, String newRefreshToken) {}

    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.jwt.refresh-token-days}")
    private long refreshTokenDays;

    /** Creates a token, stores its hash, and returns the raw value (the only time it exists). */
    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        repository.save(token);
        return raw;
    }

    /** Validates the token, revokes it, and issues a replacement. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Rotated rotate(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        User user = token.getUser();

        if (token.isRevoked()) {
            // A used token came back: treat as theft and kill every session of this user.
            repository.revokeAllByUserId(user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            repository.revokeAllByUserId(user.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        token.setRevoked(true);
        return new Rotated(user, create(user));
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(token -> token.setRevoked(true));
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}