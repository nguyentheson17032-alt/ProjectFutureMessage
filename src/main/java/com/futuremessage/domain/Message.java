package com.futuremessage.domain;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "recipient_email", nullable = false, length = 320)
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private RecipientType recipientType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(name = "unlock_at", nullable = false)
    private Instant unlockAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.LOCKED;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = User.normalizeEmail(recipientEmail);
    }

    /**
     * {@code openedAt} chỉ ghi một lần. Lần mở sau (idempotent) không được đè timestamp cũ.
     */
    public void setOpenedAt(Instant openedAt) {
        if (this.openedAt != null) {
            return;
        }
        this.openedAt = openedAt;
    }

    /**
     * Tạo message mới: luôn {@code LOCKED}, {@code unlockAt} phải ở tương lai.
     */
    public static Message compose(
            User sender,
            String title,
            String content,
            Instant unlockAt,
            String recipientEmail,
            User existingRecipient,
            Instant now
    ) {
        Instant futureUnlockAt = MessageRules.requireFutureUnlockAt(unlockAt, now);
        String email = MessageRules.resolveRecipientEmail(sender, recipientEmail);
        User linkedRecipient = existingRecipient != null && existingRecipient.hasEmail(email)
                ? existingRecipient
                : null;
        return Message.builder()
                .sender(sender)
                .recipientEmail(email)
                .recipientUser(linkedRecipient)
                .recipientType(MessageRules.recipientType(sender, email))
                .title(title == null ? null : title.trim())
                .content(content == null ? null : content.trim())
                .unlockAt(futureUnlockAt)
                .status(MessageStatus.LOCKED)
                .notificationStatus(NotificationStatus.PENDING)
                .build();
    }

    public boolean canEdit() {
        return status == MessageStatus.LOCKED;
    }

    public boolean canCancel() {
        return status == MessageStatus.LOCKED;
    }

    public boolean canOpen() {
        return status == MessageStatus.AVAILABLE;
    }

    public boolean isContentVisibleToRecipient() {
        return status == MessageStatus.AVAILABLE || status == MessageStatus.OPENED;
    }

    /**
     * Chỉ sửa title/content/unlockAt khi còn {@code LOCKED}. {@code unlockAt} mới vẫn phải ở tương lai.
     */
    public void applyEdit(String newTitle, String newContent, Instant newUnlockAt, Instant now) {
        if (!canEdit()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_EDITABLE);
        }
        if (newTitle != null) {
            String trimmed = newTitle.trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title must not be blank");
            }
            this.title = trimmed;
        }
        if (newContent != null) {
            String trimmed = newContent.trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must not be blank");
            }
            this.content = trimmed;
        }
        if (newUnlockAt != null) {
            this.unlockAt = MessageRules.requireFutureUnlockAt(newUnlockAt, now);
        }
    }

    public void cancel() {
        if (!canCancel()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_EDITABLE);
        }
        this.status = MessageStatus.CANCELLED;
    }

    /**
     * Người nhận mở lần đầu: {@code AVAILABLE → OPENED} và ghi {@code openedAt}.
     * Đã {@code OPENED} thì no-op (idempotent, không đè {@code openedAt}).
     */
    public void open(Instant now) {
        if (status == MessageStatus.OPENED) {
            return;
        }
        if (status == MessageStatus.LOCKED) {
            throw new BusinessException(ErrorCode.MESSAGE_LOCKED);
        }
        if (!canOpen()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_AVAILABLE);
        }
        this.status = MessageStatus.OPENED;
        setOpenedAt(now);
    }

    /**
     * Scheduler gọi khi {@code unlockAt <= now}: {@code LOCKED → AVAILABLE}.
     * Đã {@code AVAILABLE} thì no-op để job retry an toàn.
     */
    public void markAvailable(Instant now) {
        if (status == MessageStatus.AVAILABLE) {
            return;
        }
        if (status != MessageStatus.LOCKED) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_AVAILABLE);
        }
        if (now == null || now.isBefore(unlockAt)) {
            throw new BusinessException(ErrorCode.MESSAGE_LOCKED);
        }
        this.status = MessageStatus.AVAILABLE;
    }

    /**
     * Chỉ gửi email khi message đã mở khóa ({@code AVAILABLE} / {@code OPENED})
     * và chưa từng gửi thành công ({@code SENT} không gửi lại).
     */
    public boolean canNotify() {
        return (status == MessageStatus.AVAILABLE || status == MessageStatus.OPENED)
                && notificationStatus != NotificationStatus.SENT;
    }

    /**
     * Đánh dấu đã gửi email thành công. Đã {@code SENT} thì no-op (không đè {@code notifiedAt}).
     */
    public void markNotificationSent(Instant now) {
        if (notificationStatus == NotificationStatus.SENT) {
            return;
        }
        if (status != MessageStatus.AVAILABLE && status != MessageStatus.OPENED) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_AVAILABLE);
        }
        if (now == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "notifiedAt requires a timestamp");
        }
        this.notificationStatus = NotificationStatus.SENT;
        this.notifiedAt = now;
    }

    /**
     * SMTP fail → {@code FAILED} để job lần sau retry. Không bao giờ hạ {@code SENT} xuống {@code FAILED}.
     */
    public void markNotificationFailed() {
        if (notificationStatus == NotificationStatus.SENT) {
            return;
        }
        this.notificationStatus = NotificationStatus.FAILED;
    }

    /**
     * Admin đưa email {@code FAILED} trở lại {@code PENDING} để job gửi lại.
     * Chỉ khi message đã mở khóa ({@code AVAILABLE}/{@code OPENED}) và chưa {@code SENT}.
     */
    public void queueNotificationRetry() {
        if (notificationStatus != NotificationStatus.FAILED) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
        }
        if (status != MessageStatus.AVAILABLE && status != MessageStatus.OPENED) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_AVAILABLE);
        }
        this.notificationStatus = NotificationStatus.PENDING;
    }

    /**
     * Khi user đăng ký bằng email đã từng là recipient: gắn {@code recipientUser} nếu chưa có.
     * Không ghi đè nếu message đã thuộc user khác.
     */
    public boolean claimRecipient(User user) {
        if (user == null || recipientUser != null || !user.hasEmail(recipientEmail)) {
            return false;
        }
        this.recipientUser = user;
        return true;
    }

    public boolean isSentBy(User user) {
        return sender != null && user != null && sender.equals(user);
    }

    public boolean isAddressedTo(User user) {
        if (user == null) {
            return false;
        }
        if (recipientUser != null && recipientUser.equals(user)) {
            return true;
        }
        return user.hasEmail(recipientEmail);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = MessageStatus.LOCKED;
        }
        if (notificationStatus == null) {
            notificationStatus = NotificationStatus.PENDING;
        }
        recipientEmail = User.normalizeEmail(recipientEmail);
        if (title != null) {
            title = title.trim();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        recipientEmail = User.normalizeEmail(recipientEmail);
        if (title != null) {
            title = title.trim();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", status=" + status + ", unlockAt=" + unlockAt + "}";
    }
}
