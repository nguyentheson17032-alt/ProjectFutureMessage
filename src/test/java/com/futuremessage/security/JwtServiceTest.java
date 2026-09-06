package com.futuremessage.security;

import com.futuremessage.config.JwtProperties;
import com.futuremessage.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final JwtProperties PROPERTIES = new JwtProperties(
            "t".repeat(32),
            Duration.ofMinutes(1),
            Duration.ofDays(7)
    );

    @Test
    void createAndParseRoundTrip() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T01:00:00Z"), ZoneOffset.UTC);
        JwtService jwtService = new JwtService(PROPERTIES, clock);
        User user = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("ada@example.com")
                .build();

        String token = jwtService.createAccessToken(user);
        UserPrincipal principal = jwtService.parseAccessToken(token);

        assertThat(principal.id()).isEqualTo(user.getId());
        assertThat(principal.email()).isEqualTo("ada@example.com");
        assertThat(jwtService.accessTokenTtl()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void expiredTokenIsRejected() {
        Instant issuedAt = Instant.parse("2026-09-06T01:00:00Z");
        JwtService issuer = new JwtService(PROPERTIES, Clock.fixed(issuedAt, ZoneOffset.UTC));
        User user = User.builder().id(UUID.randomUUID()).email("ada@example.com").build();
        String token = issuer.createAccessToken(user);

        JwtService later = new JwtService(PROPERTIES, Clock.fixed(issuedAt.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.parseAccessToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(PROPERTIES, Clock.systemUTC());
        User user = User.builder().id(UUID.randomUUID()).email("ada@example.com").build();
        String token = jwtService.createAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(token + "x")).isInstanceOf(JwtException.class);
    }
}
