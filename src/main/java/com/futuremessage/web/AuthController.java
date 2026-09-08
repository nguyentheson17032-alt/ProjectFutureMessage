package com.futuremessage.web;

import com.futuremessage.service.AuthService;
import com.futuremessage.web.dto.AuthResponse;
import com.futuremessage.web.dto.LoginRequest;
import com.futuremessage.web.dto.LogoutRequest;
import com.futuremessage.web.dto.RefreshRequest;
import com.futuremessage.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public register, login, refresh, and logout. No Bearer token required.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Creates an account and returns access + refresh tokens. Duplicate email → 409 EMAIL_ALREADY_EXISTS.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns access + refresh tokens. Wrong email or password → 401 INVALID_CREDENTIALS (same message either way).")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Rotates the refresh token and issues a new access token. Reusing an old refresh token → 401 REFRESH_TOKEN_REUSED.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Deletes the refresh token so it cannot be used again. Access token still expires on its own TTL.")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }
}
