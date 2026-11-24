CREATE TABLE daily_session_db (

    id                  UUID PRIMARY KEY,
    chat_id             BIGINT NOT NULL,
    session_date        DATE NOT NULL,
    completed           BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_daily_session_chat
        FOREIGN KEY (chat_id)
            REFERENCES telegram_chat_db(id),
    CONSTRAINT uq_daily_session_chat_date
        UNIQUE (chat_id, session_date)
);


CREATE TABLE practice_exercise_db (

    id                      UUID PRIMARY KEY,
    session_id              UUID NOT NULL,
    game_id                 UUID NOT NULL,
    phase                   VARCHAR(32) NOT NULL,
    position_order          INT NOT NULL,
    completed               BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_practice_exercise_session
        FOREIGN KEY (session_id)
            REFERENCES daily_session_db(id),
    CONSTRAINT fk_practice_exercise_game
        FOREIGN KEY (game_id)
            REFERENCES game_analysis_results_db(id)
);


CREATE TABLE practice_preferences_db (

    chat_id             BIGINT PRIMARY KEY,
    top_variants        INT NOT NULL DEFAULT 3,
    continuation_depth  INT NOT NULL DEFAULT 4,

    CONSTRAINT fk_practice_preferences_chat
        FOREIGN KEY (chat_id)
            REFERENCES telegram_chat_db(id)
);
