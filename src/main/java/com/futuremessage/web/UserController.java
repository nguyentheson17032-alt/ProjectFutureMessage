package com.futuremessage.web;

import com.futuremessage.security.UserPrincipal;
import com.futuremessage.service.AuthService;
import com.futuremessage.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current authenticated user.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Requires a valid access token. Missing token → 401 UNAUTHORIZED. Bad token → 401 INVALID_TOKEN.")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return authService.getCurrentUser(principal.id());
    }
}
