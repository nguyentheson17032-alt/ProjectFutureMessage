package com.futuremessage.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.service.NotificationService;
import com.futuremessage.service.UnlockService;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.LoginRequest;
import com.futuremessage.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API + Flyway + native {@code FOR UPDATE SKIP LOCKED} against real PostgreSQL 16.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
@Import(PostgresIntegrationTest.MailStubConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    static {
        // docker-java on the classpath still defaults to API 1.32 unless this is set.
        // Docker Engine 29 (Docker Desktop 4.52+) requires at least 1.44.
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("future_message")
            .withUsername("future")
            .withPassword("future");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UnlockService unlockService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RecordingMailNotificationSender mailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearMail() {
        mailSender.clear();
    }

    @Test
    void flywayMigrationsRanAgainstPostgres() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );
        assertThat(applied).isGreaterThanOrEqualTo(5);

        List<UUID> skipLockedIds = jdbcTemplate.query(
                """
                SELECT id FROM messages
                WHERE status = 'LOCKED' AND unlock_at <= NOW()
                ORDER BY unlock_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
        assertThat(skipLockedIds).isNotNull();
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String email = uniqueEmail("ada");
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password1", "Ada"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        String accessToken = json(registered).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Ada"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void createSelfAndOtherMessages() throws Exception {
        String adaEmail = uniqueEmail("ada");
        String bobEmail = uniqueEmail("bob");
        String adaToken = register(adaEmail, "Ada");
        Instant unlockAt = Instant.now().plus(30, ChronoUnit.DAYS);

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("To me", "Keep going", unlockAt, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientType").value("SELF"))
                .andExpect(jsonPath("$.recipientEmail").value(adaEmail))
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.content").value("Keep going"));

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("For Bob", "A sealed letter", unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientType").value("OTHER"))
                .andExpect(jsonPath("$.recipientEmail").value(bobEmail))
                .andExpect(jsonPath("$.recipientUserId").value(nullValue()))
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void inboxHidesContentWhileLockedThenUnlockOpenAndNotify() throws Exception {
        String adaEmail = uniqueEmail("ada");
        String bobEmail = uniqueEmail("bob");
        String adaToken = register(adaEmail, "Ada");
        String bobToken = register(bobEmail, "Bob");
        Instant unlockAt = Instant.now().plus(14, ChronoUnit.DAYS);
        String secret = "secret-body-" + UUID.randomUUID();

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("For Bob", secret, unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andReturn();
        UUID messageId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(get("/api/v1/messages/inbox")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId.toString()))
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

        backdateUnlockAt(messageId);
        cancelOtherDueLocked(messageId);
        assertThat(unlockService.unlockDueMessages()).isEqualTo(1);

        Message unlocked = messageRepository.findById(messageId).orElseThrow();
        assertThat(unlocked.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
        assertThat(unlocked.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(unlocked.getOpenedAt()).isNull();

        mockMvc.perform(get("/api/v1/messages/inbox")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value(secret));

        assertThat(notificationService.sendPendingNotifications()).isGreaterThanOrEqualTo(1);
        assertThat(mailSender.sent()).anySatisfy(mail -> {
            assertThat(mail.to()).isEqualTo(bobEmail);
            assertThat(mail.content().subject()).contains("For Bob");
            assertThat(mail.content().textBody()).doesNotContain(secret);
            assertThat(mail.content().htmlBody()).doesNotContain(secret);
        });

        Message notified = messageRepository.findById(messageId).orElseThrow();
        assertThat(notified.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notified.getNotifiedAt()).isNotNull();

        assertThat(notificationService.sendPendingNotifications()).isZero();
        assertThat(mailSender.sent()).filteredOn(mail -> mail.to().equals(bobEmail)).hasSize(1);

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/open")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPENED"))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.content").value(secret));
        Instant openedAt = messageRepository.findById(messageId).orElseThrow().getOpenedAt();
        assertThat(openedAt).isNotNull();

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/open")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPENED"));

        Instant openedAtAgain = messageRepository.findById(messageId).orElseThrow().getOpenedAt();
        assertThat(openedAtAgain).isEqualTo(openedAt);
    }

    @Test
    void skipLockedAllowsOnlyOneWorkerToUnlockTheSameDueRow() throws Exception {
        String adaEmail = uniqueEmail("ada");
        String bobEmail = uniqueEmail("bob");
        String adaToken = register(adaEmail, "Ada");
        Instant unlockAt = Instant.now().plus(7, ChronoUnit.DAYS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("Race", "one row", unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID messageId = UUID.fromString(json(created).get("id").asText());
        backdateUnlockAt(messageId);
        cancelOtherDueLocked(messageId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> unlockService.unlockDueMessages());
            Future<Integer> second = pool.submit(() -> unlockService.unlockDueMessages());
            assertThat(first.get() + second.get()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        Message stored = messageRepository.findById(messageId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
    }

    @Test
    void registeringRecipientBackfillsRecipientUserId() throws Exception {
        String adaEmail = uniqueEmail("ada");
        String bobEmail = uniqueEmail("bob");
        String adaToken = register(adaEmail, "Ada");
        Instant unlockAt = Instant.now().plus(10, ChronoUnit.DAYS);

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", bearer(adaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMessageRequest("Later", "hello", unlockAt, bobEmail))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientUserId").value(nullValue()))
                .andReturn();
        UUID messageId = UUID.fromString(json(created).get("id").asText());

        String bobToken = register(bobEmail, "Bob");
        JsonNode bobMe = json(mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(get("/api/v1/messages/" + messageId)
                        .header("Authorization", bearer(adaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientUserId").value(bobMe.get("id").asText()));
    }

    private void backdateUnlockAt(UUID messageId) {
        int updated = jdbcTemplate.update(
                "UPDATE messages SET unlock_at = NOW() - INTERVAL '1 minute' WHERE id = ?",
                messageId
        );
        assertThat(updated).isEqualTo(1);
    }

    /** Unlock job selects every due LOCKED row; cancel leftovers so this test owns the batch. */
    private void cancelOtherDueLocked(UUID keepId) {
        jdbcTemplate.update(
                """
                UPDATE messages
                SET status = 'CANCELLED'
                WHERE status = 'LOCKED'
                  AND unlock_at <= NOW()
                  AND id <> ?
                """,
                keepId
        );
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password1", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private static String uniqueEmail(String localPart) {
        return localPart + "-" + UUID.randomUUID() + "@example.com";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailStubConfig {

        @Bean
        @Primary
        RecordingMailNotificationSender recordingMailNotificationSender() {
            return new RecordingMailNotificationSender();
        }
    }
}
