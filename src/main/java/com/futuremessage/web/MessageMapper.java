package com.futuremessage.web;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageRules;
import com.futuremessage.domain.User;
import com.futuremessage.web.dto.MessageResponse;
import com.futuremessage.web.dto.MessageSummaryResponse;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message, User viewer) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getRecipientEmail(),
                message.getRecipientUser() == null ? null : message.getRecipientUser().getId(),
                message.getRecipientType(),
                message.getTitle(),
                visibleContent(message, viewer),
                message.getUnlockAt(),
                message.getStatus(),
                message.getOpenedAt(),
                message.getNotificationStatus(),
                message.getNotifiedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    public static MessageSummaryResponse toSummary(Message message, User viewer) {
        return new MessageSummaryResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getRecipientEmail(),
                message.getRecipientType(),
                message.getTitle(),
                visibleContent(message, viewer),
                message.getUnlockAt(),
                message.getStatus(),
                message.getOpenedAt(),
                message.getCreatedAt()
        );
    }

    /** Ẩn/hiện content theo {@link MessageRules#visibleContent}. */
    public static String visibleContent(Message message, User viewer) {
        return MessageRules.visibleContent(message, viewer);
    }
}
