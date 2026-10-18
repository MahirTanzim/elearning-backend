package com.example.elearningbackend.security;

import com.example.elearningbackend.auth.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleAuthService googleAuthService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser google = (OidcUser) authentication.getPrincipal();

        if (google.getEmail() == null || !Boolean.TRUE.equals(google.getEmailVerified())) {
            response.sendRedirect(frontendUrl + "/login?error=email_not_verified");
            return;
        }

        try {
            GoogleAuthService.Tokens tokens = googleAuthService.loginOrRegister(
                google.getEmail(),
                google.getFullName(),
                google.getPicture(),
                google.getSubject()
            );

            response.sendRedirect(frontendUrl + "/oauth2/success"
                + "#accessToken=" + encode(tokens.accessToken())
                + "&refreshToken=" + encode(tokens.refreshToken()));
        } catch (ResponseStatusException e) {
            response.sendRedirect(frontendUrl + "/login?error=account_not_active");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}