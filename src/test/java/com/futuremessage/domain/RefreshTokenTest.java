package com.futuremessage.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void isActiveUntilExpiryAndRevocation() {
        Instant now = Instant.parse("2026-09-05T12:00:00Z");
        RefreshToken token = RefreshToken.builder()
                .tokenHash("abc")
                .expiresAt(now.plusSeconds(60))
                .build();

        assertThat(token.isActive(now)).isTrue();
        assertThat(token.isExpired(now.plusSeconds(60))).isTrue();

        token.revoke(now);
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isActive(now)).isFalse();
    }

    @Test
    void revokeDoesNotOverwriteExistingRevokedAt() {
        Instant first = Instant.parse("2026-09-05T12:00:00Z");
        Instant later = first.plusSeconds(10);
        RefreshToken token = RefreshToken.builder()
                .tokenHash("abc")
                .expiresAt(first.plusSeconds(60))
                .build();

        token.revoke(first);
        token.revoke(later);

        assertThat(token.getRevokedAt()).isEqualTo(first);
    }
}
