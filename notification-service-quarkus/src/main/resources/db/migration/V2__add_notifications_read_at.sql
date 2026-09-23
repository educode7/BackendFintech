-- V2__add_notifications_read_at.sql
ALTER TABLE notifications ADD COLUMN read_at TIMESTAMPTZ;

CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE read_at IS NULL;
