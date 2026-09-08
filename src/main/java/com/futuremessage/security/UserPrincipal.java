package com.futuremessage.security;

import com.futuremessage.domain.UserRole;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.UUID;

@Hidden
public record UserPrincipal(UUID id, String email, UserRole role) {

    public UserPrincipal {
        role = role == null ? UserRole.USER : role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
