package com.futuremessage.domain;

/**
 * Trạng thái email thông báo khi message chuyển sang {@link MessageStatus#AVAILABLE}.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
