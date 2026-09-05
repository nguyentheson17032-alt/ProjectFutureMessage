package com.futuremessage.domain;

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
