package com.futuremessage.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.LoginRequest;
import com.futuremessage.web.dto.RegisterRequest;
import com.futuremessage.web.dto.UpdateUserEnabledRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanReadStatsUsersMessagesAndRetryFailedNotification() throws Exception {
        String adminToken = loginAsAdmin();
        String adaEmail = "ada-admin-" + UUID.randomUUID() + "@example.com";
        String bobEmail = "bob-admin-" + UUID.randomUUID() + "@example.com";
        String adaToken = register(adaEmail, "Ada");
        register(bobEmail, "Bob");
        Instant unlockAt = Instant.now().plus(2, ChronoUnit.HOURS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("For Bob", "sealed-secret", unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID messageId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.messagesByStatus.locked").isNumber())
                .andExpect(jsonPath("$.notifications.pending").isNumber())
                .andExpect(jsonPath("$.unlockingWithin24Hours").isNumber());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("q", adaEmail)
                        .param("role", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value(adaEmail))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());

        User ada = userRepository.findByEmail(adaEmail).orElseThrow();
        mockMvc.perform(get("/api/v1/admin/users/" + ada.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(adaEmail))
                .andExpect(jsonPath("$.sentCount").value(1))
                .andExpect(jsonPath("$.inboxCount").value(0));

        mockMvc.perform(get("/api/v1/admin/messages")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "LOCKED")
                        .param("senderEmail", adaEmail)
                        .param("recipientEmail", bobEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.items[0].notificationStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/admin/messages/" + messageId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.content").doesNotExist());

        Message message = messageRepository.findDetailedById(messageId).orElseThrow();
        message.setStatus(MessageStatus.AVAILABLE);
        message.setNotificationStatus(NotificationStatus.FAILED);
        messageRepository.save(message);

        mockMvc.perform(get("/api/v1/admin/messages/" + messageId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("sealed-secret"));

        mockMvc.perform(post("/api/v1/admin/messages/" + messageId + "/retry-notification")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationStatus").value("PENDING"));

        Message afterRetry = messageRepository.findDetailedById(messageId).orElseThrow();
        afterRetry.setNotificationStatus(NotificationStatus.SENT);
        messageRepository.save(afterRetry);

        mockMvc.perform(post("/api/v1/admin/messages/" + messageId + "/retry-notification")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_RETRYABLE"));
    }

    @Test
    void adminCanDisableUserSoLoginFails() throws Exception {
        String adminToken = loginAsAdmin();
        String email = "disable-" + UUID.randomUUID() + "@example.com";
        register(email, "Ada");
        User user = userRepository.findByEmail(email).orElseThrow();

        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserEnabledRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void adminUserListSupportsPagination() throws Exception {
        String adminToken = loginAsAdmin();
        register("page-a-" + UUID.randomUUID() + "@example.com", "Ada");
        register("page-b-" + UUID.randomUUID() + "@example.com", "Bob");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    private String loginAsAdmin() throws Exception {
        String email = "admin-api-" + UUID.randomUUID() + "@example.com";
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password1"))
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password1"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password1", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
