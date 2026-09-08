package com.futuremessage.repository;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @EntityGraph(attributePaths = {"sender", "recipientUser"})
    @Query("select m from Message m where m.id = :id")
    Optional<Message> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"sender", "recipientUser"})
    Page<Message> findBySender_Id(UUID senderId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "recipientUser"})
    @Query("""
            select m from Message m
            where m.status <> com.futuremessage.domain.MessageStatus.CANCELLED
              and (m.recipientUser.id = :userId or m.recipientEmail = :email)
            """)
    Page<Message> findInbox(@Param("userId") UUID userId, @Param("email") String email, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Message m set m.recipientUser = :user where m.recipientEmail = :email and m.recipientUser is null")
    int linkUnclaimedMessagesToUser(@Param("user") User user, @Param("email") String email);

    /**
     * Lấy id các message {@code LOCKED} đã đến {@code unlock_at}, khóa hàng đến hết transaction.
     * {@code SKIP LOCKED}: instance / transaction khác đang xử lý hàng đó thì bỏ qua, không chờ.
     * Dùng index {@code idx_messages_status_unlock_at}. Phải gọi trong {@code @Transactional}.
     */
    @Query(value = """
            SELECT id
            FROM messages
            WHERE status = 'LOCKED'
              AND unlock_at <= :now
            ORDER BY unlock_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockDueLockedMessageIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    /**
     * Message đã mở khóa, email còn {@code PENDING} hoặc {@code FAILED} (retry).
     * Không lấy {@code SENT} — không gửi lại. {@code SKIP LOCKED} tránh hai instance gửi trùng.
     */
    @Query(value = """
            SELECT id
            FROM messages
            WHERE status IN ('AVAILABLE', 'OPENED')
              AND notification_status IN ('PENDING', 'FAILED')
            ORDER BY unlock_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockPendingNotificationMessageIds(@Param("batchSize") int batchSize);

    @EntityGraph(attributePaths = {"sender"})
    @Query("select m from Message m where m.id in :ids")
    List<Message> findDetailedByIdIn(@Param("ids") List<UUID> ids);

    long countByStatus(MessageStatus status);

    long countByNotificationStatus(NotificationStatus notificationStatus);

    Optional<Message> findFirstByTitle(String title);

    long countBySender_Id(UUID senderId);

    @Query("""
            select count(m) from Message m
            where m.status <> com.futuremessage.domain.MessageStatus.CANCELLED
              and (m.recipientUser.id = :userId or m.recipientEmail = :email)
            """)
    long countInbox(@Param("userId") UUID userId, @Param("email") String email);

    @Query("""
            select count(m) from Message m
            where m.status = com.futuremessage.domain.MessageStatus.LOCKED
              and m.unlockAt > :now
              and m.unlockAt <= :until
            """)
    long countLockedUnlockingBetween(@Param("now") Instant now, @Param("until") Instant until);

    @EntityGraph(attributePaths = {"sender", "recipientUser"})
    @Query(
            value = """
                    select m from Message m
                    where (:status is null or m.status = :status)
                      and (:notificationStatus is null or m.notificationStatus = :notificationStatus)
                      and (:senderEmail is null or m.sender.email = :senderEmail)
                      and (:recipientEmail is null or m.recipientEmail = :recipientEmail)
                    """,
            countQuery = """
                    select count(m) from Message m
                    where (:status is null or m.status = :status)
                      and (:notificationStatus is null or m.notificationStatus = :notificationStatus)
                      and (:senderEmail is null or m.sender.email = :senderEmail)
                      and (:recipientEmail is null or m.recipientEmail = :recipientEmail)
                    """
    )
    Page<Message> searchForAdmin(
            @Param("status") MessageStatus status,
            @Param("notificationStatus") NotificationStatus notificationStatus,
            @Param("senderEmail") String senderEmail,
            @Param("recipientEmail") String recipientEmail,
            Pageable pageable
    );
}
