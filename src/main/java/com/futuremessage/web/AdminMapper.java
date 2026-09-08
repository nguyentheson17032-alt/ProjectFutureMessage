package com.futuremessage.web;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageRules;
import com.futuremessage.web.dto.AdminMessageResponse;
import com.futuremessage.web.dto.AdminMessageSummaryResponse;

public final class AdminMapper {

    private AdminMapper() {
    }

    public static AdminMessageSummaryResponse toSummary(Message message) {
        return new AdminMessageSummaryResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getEmail(),
                message.getSender().getDisplayName(),
                message.getRecipientEmail(),
                message.getRecipientUser() == null ? null : message.getRecipientUser().getId(),
                message.getRecipientType(),
                message.getTitle(),
                message.getUnlockAt(),
                message.getStatus(),
                message.getOpenedAt(),
                message.getNotificationStatus(),
                message.getNotifiedAt(),
                message.getCreatedAt()
        );
    }

    public static AdminMessageResponse toResponse(Message message) {
        return new AdminMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getEmail(),
                message.getSender().getDisplayName(),
                message.getRecipientEmail(),
                message.getRecipientUser() == null ? null : message.getRecipientUser().getId(),
                message.getRecipientType(),
                message.getTitle(),
                MessageRules.visibleContentForAdmin(message),
                message.getUnlockAt(),
                message.getStatus(),
                message.getOpenedAt(),
                message.getNotificationStatus(),
                message.getNotifiedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
