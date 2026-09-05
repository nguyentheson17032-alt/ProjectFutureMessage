CREATE INDEX idx_messages_status_unlock_at
    ON messages (status, unlock_at);

CREATE INDEX idx_messages_recipient_email
    ON messages (recipient_email);

CREATE INDEX idx_messages_sender_id_created_at
    ON messages (sender_id, created_at DESC);

CREATE INDEX idx_messages_recipient_user_id_status
    ON messages (recipient_user_id, status);
