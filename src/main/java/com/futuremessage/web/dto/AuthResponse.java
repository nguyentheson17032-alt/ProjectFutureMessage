package com.futuremessage.web.dto;

import java.time.Duration;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken, Duration accessTokenTtl, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", accessTokenTtl.toSeconds(), user);
    }
}
