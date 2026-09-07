package com.futuremessage.domain;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;

import java.time.Instant;

/**
 * Quy tắc nghiệp vụ liên quan đến <em>ai được làm gì</em> và <em>nội dung có hiện không</em>.
 * <p>
 * Chuyển trạng thái của chính message ({@code LOCKED → AVAILABLE → OPENED}) nằm ở
 * {@link Message} để entity không bị sửa lệch invariant bằng setter.
 */
public final class MessageRules {

    private MessageRules() {
    }

    /**
     * {@code unlockAt} phải ở tương lai tại thời điểm tạo hoặc khi đổi hạn mở.
     */
    public static Instant requireFutureUnlockAt(Instant unlockAt, Instant now) {
        if (unlockAt == null || now == null || !unlockAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
        }
        return unlockAt;
    }

    /**
     * Chỉ người gửi mới được sửa / hủy message.
     */
    public static void requireSender(Message message, User actor) {
        if (!message.isSentBy(actor)) {
            throw new BusinessException(ErrorCode.NOT_SENDER);
        }
    }

    /**
     * Chỉ người nhận (email khớp user đang đăng nhập) mới được mở message.
     */
    public static void requireRecipient(Message message, User actor) {
        if (!message.isAddressedTo(actor)) {
            throw new BusinessException(ErrorCode.NOT_RECIPIENT);
        }
    }

    /**
     * Người lạ không được biết message tồn tại — trả 404 thay vì 403.
     */
    public static void requireParticipant(Message message, User actor) {
        if (!message.isSentBy(actor) && !message.isAddressedTo(actor)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
    }

    /**
     * Bỏ trống {@code recipientEmail} → gửi cho chính mình.
     * Luôn normalize lowercase để khớp unique email.
     */
    public static String resolveRecipientEmail(User sender, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return sender.getEmail();
        }
        return User.normalizeEmail(recipientEmail);
    }

    public static RecipientType recipientType(User sender, String recipientEmail) {
        return sender.hasEmail(recipientEmail) ? RecipientType.SELF : RecipientType.OTHER;
    }

    /**
     * Người gửi luôn xem được content (kể cả {@code LOCKED}).
     * Người nhận chỉ xem khi {@code AVAILABLE} hoặc {@code OPENED}.
     */
    public static String visibleContent(Message message, User viewer) {
        if (message.isSentBy(viewer)) {
            return message.getContent();
        }
        if (message.isAddressedTo(viewer) && message.isContentVisibleToRecipient()) {
            return message.getContent();
        }
        return null;
    }
}
