package com.futuremessage.security;

import com.futuremessage.config.JwtProperties;
import com.futuremessage.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = properties.signingKey();
    }

    public String createAccessToken(User user) {
        Date issuedAt = Date.from(clock.instant());
        Date expiresAt = Date.from(clock.instant().plus(properties.accessTokenTtl()));
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    public UserPrincipal parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new UserPrincipal(UUID.fromString(claims.getSubject()), claims.get(EMAIL_CLAIM, String.class));
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }
}
