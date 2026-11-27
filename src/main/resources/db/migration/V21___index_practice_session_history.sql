CREATE INDEX idx_daily_session_history
    ON daily_session_db (chat_id, created_at DESC, id DESC);


ALTER TABLE practice_exercise_db
    DROP COLUMN selected_variant,
    DROP COLUMN step;

ALTER TABLE practice_exercise_db RENAME COLUMN current_depth TO required_depth;
