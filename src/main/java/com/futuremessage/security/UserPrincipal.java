package com.futuremessage.security;

import io.swagger.v3.oas.annotations.Hidden;

import java.util.UUID;

@Hidden
public record UserPrincipal(UUID id, String email) {
}
