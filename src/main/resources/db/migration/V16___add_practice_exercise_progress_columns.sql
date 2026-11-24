ALTER TABLE practice_exercise_db
    ADD COLUMN top_variants    INT NOT NULL DEFAULT 3,
    ADD COLUMN current_depth   INT NOT NULL DEFAULT 4,
    ADD COLUMN selected_variant INT,
    ADD COLUMN step            INT NOT NULL DEFAULT 1;
