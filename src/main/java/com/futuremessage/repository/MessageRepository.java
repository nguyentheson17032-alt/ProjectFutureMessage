package com.futuremessage.repository;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
