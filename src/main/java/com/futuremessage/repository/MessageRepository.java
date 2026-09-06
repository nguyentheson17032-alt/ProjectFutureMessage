package com.futuremessage.repository;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("update Message m set m.recipientUser = :user where m.recipientEmail = :email and m.recipientUser is null")
    int linkUnclaimedMessagesToUser(@Param("user") User user, @Param("email") String email);
}
