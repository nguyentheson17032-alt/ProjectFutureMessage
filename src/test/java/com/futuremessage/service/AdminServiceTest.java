package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.web.dto.AdminStatsResponse;
import com.futuremessage.web.dto.AdminUserDetailResponse;
import com.futuremessage.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;

    private AdminService adminService;
    private User member;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                messageRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        member = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada", UserRole.USER);
    }

    @Test
    void statsAggregatesCountsAndUnlockWindow() {
        when(userRepository.count()).thenReturn(12L);
        when(messageRepository.countByStatus(MessageStatus.LOCKED)).thenReturn(4L);
        when(messageRepository.countByStatus(MessageStatus.AVAILABLE)).thenReturn(1L);
        when(messageRepository.countByStatus(MessageStatus.OPENED)).thenReturn(6L);
        when(messageRepository.countByStatus(MessageStatus.CANCELLED)).thenReturn(2L);
        when(messageRepository.countByNotificationStatus(NotificationStatus.PENDING)).thenReturn(3L);
        when(messageRepository.countByNotificationStatus(NotificationStatus.SENT)).thenReturn(8L);
        when(messageRepository.countByNotificationStatus(NotificationStatus.FAILED)).thenReturn(2L);
        when(messageRepository.countLockedUnlockingBetween(NOW, NOW.plus(Duration.ofHours(24)))).thenReturn(3L);

        AdminStatsResponse stats = adminService.stats();

        assertThat(stats.totalUsers()).isEqualTo(12L);
        assertThat(stats.messagesByStatus().locked()).isEqualTo(4L);
        assertThat(stats.messagesByStatus().available()).isEqualTo(1L);
        assertThat(stats.messagesByStatus().opened()).isEqualTo(6L);
        assertThat(stats.messagesByStatus().cancelled()).isEqualTo(2L);
        assertThat(stats.notifications().pending()).isEqualTo(3L);
        assertThat(stats.notifications().sent()).isEqualTo(8L);
        assertThat(stats.notifications().failed()).isEqualTo(2L);
        assertThat(stats.unlockingWithin24Hours()).isEqualTo(3L);
    }

    @Test
    void listUsersNormalizesBlankQuery() {
        when(userRepository.search(eq(""), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member)));

        List<UserResponse> items = adminService.listUsers("  ", null, null, 0, 20).items();

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().email()).isEqualTo("ada@example.com");
    }

    @Test
    void getUserIncludesSentAndInboxCounts() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(messageRepository.countBySender_Id(member.getId())).thenReturn(2L);
        when(messageRepository.countInbox(member.getId(), member.getEmail())).thenReturn(5L);

        AdminUserDetailResponse detail = adminService.getUser(member.getId());

        assertThat(detail.sentCount()).isEqualTo(2L);
        assertThat(detail.inboxCount()).isEqualTo(5L);
        assertThat(detail.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void updateUserEnabledFlushesAndReturnsProfile() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(userRepository.saveAndFlush(member)).thenReturn(member);
        when(messageRepository.countBySender_Id(member.getId())).thenReturn(0L);
        when(messageRepository.countInbox(member.getId(), member.getEmail())).thenReturn(0L);

        AdminUserDetailResponse detail = adminService.updateUserEnabled(member.getId(), false);

        assertThat(member.isEnabled()).isFalse();
        assertThat(detail.enabled()).isFalse();
        verify(userRepository).saveAndFlush(member);
    }

    @Test
    void getMessageHidesLockedContent() {
        Message message = lockedMessage();
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThat(adminService.getMessage(message.getId()).content()).isNull();
    }

    @Test
    void retryNotificationQueuesFailedAvailableMessage() {
        Message message = lockedMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        message.setNotificationStatus(NotificationStatus.FAILED);
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThat(adminService.retryNotification(message.getId()).notificationStatus())
                .isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void retryNotificationRejectsSent() {
        Message message = lockedMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        message.setNotificationStatus(NotificationStatus.SENT);
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> adminService.retryNotification(message.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
    }

    @Test
    void listUsersRejectsInvalidPageSize() {
        assertThatThrownBy(() -> adminService.listUsers(null, null, null, 0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void unknownUserIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUser(missing))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private Message lockedMessage() {
        return Message.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .sender(member)
                .recipientEmail("bob@example.com")
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(Instant.parse("2030-01-01T00:00:00Z"))
                .status(MessageStatus.LOCKED)
                .notificationStatus(NotificationStatus.PENDING)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static User user(String id, String email, String name, UserRole role) {
        return User.builder()
                .id(UUID.fromString(id))
                .email(email)
                .passwordHash("hash")
                .displayName(name)
                .role(role)
                .enabled(true)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
