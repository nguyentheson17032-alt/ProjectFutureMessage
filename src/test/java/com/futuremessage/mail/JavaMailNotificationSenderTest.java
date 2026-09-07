package com.futuremessage.mail;

import com.futuremessage.config.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaMailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private JavaMailNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new JavaMailNotificationSender(
                mailSender,
                new MailProperties("noreply@futuremessage.local", "http://localhost:3000/inbox")
        );
    }

    @Test
    void sendsMultipartMailToRecipient() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        sender.send("bob@example.com", new UnlockMailContent(
                "Tin nhắn tương lai đã sẵn sàng: Hello",
                "plain body",
                "<p>html body</p>"
        ));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Tin nhắn tương lai đã sẵn sàng: Hello");
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("bob@example.com");
        assertThat(sent.getFrom()[0].toString()).contains("noreply@futuremessage.local");
    }

    @Test
    void wrapsMailFailure() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(mimeMessage);

        assertThatThrownBy(() -> sender.send(
                "bob@example.com",
                new UnlockMailContent("s", "t", "<p>h</p>")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bob@example.com");
    }
}
