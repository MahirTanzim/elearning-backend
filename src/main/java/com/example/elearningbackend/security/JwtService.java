package com.example.elearningbackend.security;

import com.example.elearningbackend.user.Role;
import com.example.elearningbackend.user.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.access-token-minutes}")
    private long accessTokenTime;

    public long getExpiresInSeconds() {
        return accessTokenTime * 60;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("elearning")
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenTime, ChronoUnit.MINUTES))
            .claim("email", user.getEmail())
            .claim("roles", roles)
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}