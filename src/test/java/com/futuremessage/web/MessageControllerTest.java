package com.futuremessage.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.RegisterRequest;
import com.futuremessage.web.dto.UpdateMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void createSentInboxDetailOpenAndCancel() throws Exception {
        String adaEmail = "ada-" + UUID.randomUUID() + "@example.com";
        String bobEmail = "bob-" + UUID.randomUUID() + "@example.com";
        String adaToken = register(adaEmail, "Ada");
        String bobToken = register(bobEmail, "Bob");
        Instant unlockAt = Instant.now().plus(30, ChronoUnit.DAYS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("For Bob", "A sealed letter", unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.recipientType").value("OTHER"))
                .andExpect(jsonPath("$.recipientEmail").value(bobEmail))
                .andExpect(jsonPath("$.content").value("A sealed letter"))
                .andReturn();

        String messageId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/messages/sent")
                        .header("Authorization", bearer(adaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId))
                .andExpect(jsonPath("$.items[0].content").value("A sealed letter"));

        mockMvc.perform(get("/api/v1/messages/inbox")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId))
                .andExpect(jsonPath("$.items[0].title").value("For Bob"))
                .andExpect(jsonPath("$.items[0].content").doesNotExist());

        mockMvc.perform(get("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").doesNotExist());

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/open")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MESSAGE_LOCKED"));

        Message stored = messageRepository.findById(UUID.fromString(messageId)).orElseThrow();
        stored.setStatus(MessageStatus.AVAILABLE);
        messageRepository.saveAndFlush(stored);

        mockMvc.perform(get("/api/v1/messages/inbox")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value("A sealed letter"));

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/open")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPENED"))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.content").value("A sealed letter"));

        Instant openedAt = messageRepository.findById(UUID.fromString(messageId)).orElseThrow().getOpenedAt();

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/open")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPENED"));

        Instant openedAtAgain = messageRepository.findById(UUID.fromString(messageId)).orElseThrow().getOpenedAt();
        assertThat(openedAtAgain).isEqualTo(openedAt);

        mockMvc.perform(patch("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMessageRequest("Nope", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MESSAGE_NOT_EDITABLE"));
    }

    @Test
    void senderCanUpdateAndCancelWhileLocked() throws Exception {
        String email = "self-" + UUID.randomUUID() + "@example.com";
        String token = register(email, "Ada");
        Instant unlockAt = Instant.now().plus(10, ChronoUnit.DAYS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("To me", "Original", unlockAt, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientType").value("SELF"))
                .andReturn();
        String messageId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMessageRequest("Updated title", "Updated body", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.content").value("Updated body"));

        mockMvc.perform(delete("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void messagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/messages/inbox"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createValidatesPayload() throws Exception {
        String token = register("val-" + UUID.randomUUID() + "@example.com", "Ada");

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","content":"","unlockAt":"2000-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void strangerCannotSeeSomeoneElsesMessage() throws Exception {
        String adaToken = register("ada2-" + UUID.randomUUID() + "@example.com", "Ada");
        String eveToken = register("eve-" + UUID.randomUUID() + "@example.com", "Eve");
        Instant unlockAt = Instant.now().plus(5, ChronoUnit.DAYS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("Private", "Nope", unlockAt, null))))
                .andExpect(status().isCreated())
                .andReturn();
        String messageId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(eveToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"));
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
