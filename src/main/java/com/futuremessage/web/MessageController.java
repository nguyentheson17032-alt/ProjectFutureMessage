package com.futuremessage.web;

import com.futuremessage.common.PageResponse;
import com.futuremessage.security.UserPrincipal;
import com.futuremessage.service.MessageService;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.MessageResponse;
import com.futuremessage.web.dto.MessageSummaryResponse;
import com.futuremessage.web.dto.UpdateMessageRequest;
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
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return messageService.create(principal.id(), request);
    }

    @GetMapping("/sent")
    public PageResponse<MessageSummaryResponse> sent(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.listSent(principal.id(), page, size);
    }

    @GetMapping("/inbox")
    public PageResponse<MessageSummaryResponse> inbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.listInbox(principal.id(), page, size);
    }

    @GetMapping("/{id}")
    public MessageResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return messageService.get(principal.id(), id);
    }

    @PatchMapping("/{id}")
    public MessageResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        return messageService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        messageService.cancel(principal.id(), id);
    }

    @PostMapping("/{id}/open")
    public MessageResponse open(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return messageService.open(principal.id(), id);
    }
}
