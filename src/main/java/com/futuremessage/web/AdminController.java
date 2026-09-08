package com.futuremessage.web;

import com.futuremessage.common.PageResponse;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.UserRole;
import com.futuremessage.service.AdminService;
import com.futuremessage.web.dto.AdminMessageResponse;
import com.futuremessage.web.dto.AdminMessageSummaryResponse;
import com.futuremessage.web.dto.AdminStatsResponse;
import com.futuremessage.web.dto.AdminUserDetailResponse;
import com.futuremessage.web.dto.UpdateUserEnabledRequest;
import com.futuremessage.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Operational dashboard for ROLE_ADMIN. Regular users receive 403 FORBIDDEN. Does not replace the user mailbox.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(
            summary = "Dashboard stats",
            description = "Total users, message counts by status, notification PENDING/SENT/FAILED, and LOCKED messages unlocking in the next 24 hours."
    )
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    @Operation(
            summary = "List users",
            description = "Paginated. `q` matches email or displayName (contains, case-insensitive). Optional `enabled` and `role` filters."
    )
    public PageResponse<UserResponse> listUsers(
            @Parameter(description = "Search email or display name") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by enabled flag") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Filter by role") @RequestParam(required = false) UserRole role,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.listUsers(q, enabled, role, page, size);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "User detail", description = "Profile plus sent count and inbox count for that email.")
    public AdminUserDetailResponse getUser(@Parameter(description = "User id") @PathVariable UUID id) {
        return adminService.getUser(id);
    }

    @PatchMapping("/users/{id}")
    @Operation(
            summary = "Enable or disable user",
            description = "Sets `enabled` only. Role cannot be changed here. Disabled users cannot login or refresh; their messages remain."
    )
    public AdminUserDetailResponse updateUser(
            @Parameter(description = "User id") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserEnabledRequest request
    ) {
        return adminService.updateUserEnabled(id, request.enabled());
    }

    @GetMapping("/messages")
    @Operation(
            summary = "List all messages",
            description = "Paginated metadata (no content). Filter by status, notificationStatus, senderEmail, recipientEmail."
    )
    public PageResponse<AdminMessageSummaryResponse> listMessages(
            @Parameter(description = "Message status") @RequestParam(required = false) MessageStatus status,
            @Parameter(description = "Notification status") @RequestParam(required = false) NotificationStatus notificationStatus,
            @Parameter(description = "Exact sender email") @RequestParam(required = false) String senderEmail,
            @Parameter(description = "Exact recipient email") @RequestParam(required = false) String recipientEmail,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.listMessages(status, notificationStatus, senderEmail, recipientEmail, page, size);
    }

    @GetMapping("/messages/{id}")
    @Operation(
            summary = "Message operational detail",
            description = "Includes content only when AVAILABLE or OPENED. LOCKED and CANCELLED omit content."
    )
    public AdminMessageResponse getMessage(@Parameter(description = "Message id") @PathVariable UUID id) {
        return adminService.getMessage(id);
    }

    @PostMapping("/messages/{id}/retry-notification")
    @Operation(
            summary = "Retry failed unlock email",
            description = "Only when notificationStatus is FAILED and status is AVAILABLE or OPENED. Sets PENDING so the notification job sends again. SENT cannot be retried."
    )
    public AdminMessageResponse retryNotification(@Parameter(description = "Message id") @PathVariable UUID id) {
        return adminService.retryNotification(id);
    }
}
