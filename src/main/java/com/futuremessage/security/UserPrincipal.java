package com.futuremessage.security;

import java.util.UUID;

public record UserPrincipal(UUID id, String email) {
}
