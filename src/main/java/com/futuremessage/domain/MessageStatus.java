package com.futuremessage.domain;

/**
 * Vòng đời của một message.
 * <p>
 * {@code LOCKED} → {@code AVAILABLE} (scheduler khi đến {@code unlockAt})
 * → {@code OPENED} (người nhận mở lần đầu).
 * {@code CANCELLED} chỉ khi người gửi hủy lúc còn {@code LOCKED}.
 */
public enum MessageStatus {
    LOCKED,
    AVAILABLE,
    OPENED,
    CANCELLED
}
