package com.futuremessage.web.dto;

import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        boolean emailVerified,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                user.roleOrDefault(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
