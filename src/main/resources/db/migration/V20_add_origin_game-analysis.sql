ALTER TABLE game_analysis_db
    ADD COLUMN origin VARCHAR(20)
        NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE game_analysis_db
    ADD CONSTRAINT chk_game_analysis_origin
        CHECK (origin IN ('PERSONAL', 'DATASET'));
