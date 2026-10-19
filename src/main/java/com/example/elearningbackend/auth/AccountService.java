package com.example.elearningbackend.auth;

import com.example.elearningbackend.security.TokenUtil;
import com.example.elearningbackend.user.User;
import com.example.elearningbackend.user.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.tokens.verification-hours}")
    private long verificationHours;

    @Value("${app.tokens.reset-minutes}")
    private long resetMinutes;

    // ---------- Email verification ----------

    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified()) return;
        if (recentlySent(user, UserToken.EMAIL_VERIFICATION)) return;

        String raw = createToken(user, UserToken.EMAIL_VERIFICATION,
            LocalDateTime.now().plusHours(verificationHours));

        sendSafely(user.getEmail(), "Verify your email",
            "Hi " + user.getFullName() + ",\n\n"
          + "Please confirm your email address by opening this link:\n\n"
          + frontendUrl + "/verify-email?token=" + raw + "\n\n"
          + "The link expires in " + verificationHours + " hours.\n"
          + "If you did not create an account, you can ignore this email.");
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(normalize(email)).ifPresent(this::sendVerificationEmail);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        UserToken token = findValid(rawToken, UserToken.EMAIL_VERIFICATION);
        token.setUsedAt(LocalDateTime.now());
        token.getUser().setEmailVerified(true);
    }

    // ---------- Password reset ----------

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalize(email)).ifPresent(user -> {
            if (!"ACTIVE".equals(user.getStatus())) return;
            if (recentlySent(user, UserToken.PASSWORD_RESET)) return;

            String raw = createToken(user, UserToken.PASSWORD_RESET,
                LocalDateTime.now().plusMinutes(resetMinutes));

            sendSafely(user.getEmail(), "Reset your password",
                "Hi " + user.getFullName() + ",\n\n"
              + "We received a request to reset your password. Open this link to choose a new one:\n\n"
              + frontendUrl + "/reset-password?token=" + raw + "\n\n"
              + "The link expires in " + resetMinutes + " minutes.\n"
              + "If you did not ask for this, you can ignore this email. Your password stays the same.");
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        UserToken token = findValid(rawToken, UserToken.PASSWORD_RESET);
        User user = token.getUser();

        if (!"ACTIVE".equals(user.getStatus())) {
            throw invalidToken();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true); // they just proved they can read this inbox

        // marks this link and any other open reset links as used
        tokenRepository.invalidateUnused(user.getId(), UserToken.PASSWORD_RESET, LocalDateTime.now());

        // log out every existing session
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ---------- helpers ----------

    private String createToken(User user, String type, LocalDateTime expiresAt) {
        // a new link cancels the previous one
        tokenRepository.invalidateUnused(user.getId(), type, LocalDateTime.now());

        String raw = TokenUtil.generateRaw();
        UserToken token = new UserToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(TokenUtil.sha256(raw));
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
        return raw;
    }

    private UserToken findValid(String rawToken, String type) {
        UserToken token = tokenRepository
            .findByTokenHashAndType(TokenUtil.sha256(rawToken), type)
            .orElseThrow(this::invalidToken);

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidToken();
        }
        return token;
    }

    private boolean recentlySent(User user, String type) {
        return tokenRepository.existsByUserIdAndTypeAndUsedAtIsNullAndCreatedAtAfter(
            user.getId(), type, LocalDateTime.now().minus(COOLDOWN));
    }

    private void sendSafely(String to, String subject, String body) {
        try {
            emailService.send(to, subject, body);
        } catch (Exception e) {
            // a mail problem must never break registration or reveal anything to the caller
            log.error("Could not send email to {}", to, e);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired link");
    }
}