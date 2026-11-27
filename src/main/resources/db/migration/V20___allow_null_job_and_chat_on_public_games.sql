ALTER TABLE game_analysis_results_db
    ALTER COLUMN job_id DROP NOT NULL;

ALTER TABLE game_analysis_results_db
    ALTER COLUMN chat_id DROP NOT NULL;
