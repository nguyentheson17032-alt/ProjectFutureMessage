package com.futuremessage.web;

import com.futuremessage.common.PageResponse;
import com.futuremessage.security.UserPrincipal;
import com.futuremessage.service.MessageService;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.MessageResponse;
import com.futuremessage.web.dto.MessageSummaryResponse;
import com.futuremessage.web.dto.UpdateMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Create, list, edit, cancel, and open locked messages. Requires Bearer access token.")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create message",
            description = "Sends a locked message. Omit `recipientEmail` (or use your own email) to send to yourself. `unlockAt` must be in the future."
    )
    public MessageResponse create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return messageService.create(principal.id(), request);
    }

    @GetMapping("/sent")
    @Operation(summary = "Sent mailbox", description = "Messages you created. Sender always sees `content`, including while LOCKED.")
    public PageResponse<MessageSummaryResponse> sent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.listSent(principal.id(), page, size);
    }

    @GetMapping("/inbox")
    @Operation(
            summary = "Inbox",
            description = "Messages addressed to your email. LOCKED items omit `content`. AVAILABLE/OPENED include `content`. CANCELLED is excluded."
    )
    public PageResponse<MessageSummaryResponse> inbox(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.listInbox(principal.id(), page, size);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Message detail",
            description = "Strangers get 404. Recipients see `content` only when AVAILABLE or OPENED. Senders always see `content`."
    )
    public MessageResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message id") @PathVariable UUID id
    ) {
        return messageService.get(principal.id(), id);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Edit message",
            description = "Sender only, and only while LOCKED. After AVAILABLE/OPENED → 409 MESSAGE_NOT_EDITABLE."
    )
    public MessageResponse update(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message id") @PathVariable UUID id,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        return messageService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancel message",
            description = "Sender only, while LOCKED. Soft-deletes to CANCELLED. Already unlocked → 409 MESSAGE_NOT_EDITABLE."
    )
    public void cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message id") @PathVariable UUID id
    ) {
        messageService.cancel(principal.id(), id);
    }

    @PostMapping("/{id}/open")
    @Operation(
            summary = "Open message",
            description = "Recipient only, when AVAILABLE. Sets OPENED + openedAt. Already OPENED is idempotent (same openedAt)."
    )
    public MessageResponse open(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message id") @PathVariable UUID id
    ) {
        return messageService.open(principal.id(), id);
    }
}
