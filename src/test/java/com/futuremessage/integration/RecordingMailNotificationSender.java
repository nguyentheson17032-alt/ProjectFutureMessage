package com.futuremessage.integration;

import com.futuremessage.mail.MailNotificationSender;
import com.futuremessage.mail.UnlockMailContent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stand-in for SMTP in Postgres integration tests. Records each send so tests
 * can assert recipient, template fields, and that message content is not leaked.
 */
final class RecordingMailNotificationSender implements MailNotificationSender {

    private final List<SentMail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, UnlockMailContent content) {
        sent.add(new SentMail(to, content));
    }

    void clear() {
        sent.clear();
    }

    List<SentMail> sent() {
        return List.copyOf(sent);
    }

    record SentMail(String to, UnlockMailContent content) {
    }
}
