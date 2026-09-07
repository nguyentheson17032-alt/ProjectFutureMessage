-- Job email: AVAILABLE/OPENED còn PENDING hoặc FAILED (retry). SENT không nằm trong index này.
CREATE INDEX idx_messages_pending_notification
    ON messages (unlock_at)
    WHERE status IN ('AVAILABLE', 'OPENED')
      AND notification_status IN ('PENDING', 'FAILED');
