ALTER TABLE daily_session_db
    ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'daily',
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

ALTER TABLE daily_session_db
    DROP CONSTRAINT uq_daily_session_chat_date;

CREATE UNIQUE INDEX uq_daily_session_chat_date_kind
    ON daily_session_db (chat_id, session_date)
    WHERE kind = 'daily';
