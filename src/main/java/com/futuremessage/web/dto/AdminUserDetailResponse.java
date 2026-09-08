package com.futuremessage.web.dto;

import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Admin user profile plus sent/inbox message counts.")
public record AdminUserDetailResponse(
        UUID id,
        String email,
        String displayName,
        boolean emailVerified,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long sentCount,
        long inboxCount
) {

    public static AdminUserDetailResponse from(User user, long sentCount, long inboxCount) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                user.roleOrDefault(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                sentCount,
                inboxCount
        );
    }
}
