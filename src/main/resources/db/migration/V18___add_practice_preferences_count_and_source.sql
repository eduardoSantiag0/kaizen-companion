ALTER TABLE practice_preferences_db
    ADD COLUMN exercise_count INT NOT NULL DEFAULT 5,
    ADD COLUMN exercise_source VARCHAR(16) NOT NULL DEFAULT 'own';
