CREATE TABLE messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id           UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    recipient_email     VARCHAR(320) NOT NULL,
    recipient_user_id   UUID REFERENCES users (id) ON DELETE SET NULL,
    recipient_type      VARCHAR(20) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    content             TEXT NOT NULL,
    unlock_at           TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    opened_at           TIMESTAMPTZ,
    notification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT messages_recipient_email_lowercase_chk CHECK (recipient_email = lower(recipient_email)),
    CONSTRAINT messages_recipient_email_not_blank_chk CHECK (length(btrim(recipient_email)) > 0),
    CONSTRAINT messages_title_not_blank_chk CHECK (length(btrim(title)) > 0),
    CONSTRAINT messages_content_not_blank_chk CHECK (length(btrim(content)) > 0),
    CONSTRAINT messages_recipient_type_chk CHECK (recipient_type IN ('SELF', 'OTHER')),
    CONSTRAINT messages_status_chk CHECK (status IN ('LOCKED', 'AVAILABLE', 'OPENED', 'CANCELLED')),
    CONSTRAINT messages_notification_status_chk CHECK (notification_status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT messages_opened_at_status_chk CHECK (
        (status = 'OPENED' AND opened_at IS NOT NULL)
        OR (status <> 'OPENED' AND opened_at IS NULL)
    ),
    CONSTRAINT messages_notified_at_chk CHECK (
        notification_status <> 'SENT' OR notified_at IS NOT NULL
    ),
    CONSTRAINT messages_version_non_negative_chk CHECK (version >= 0)
);

CREATE TRIGGER trg_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
