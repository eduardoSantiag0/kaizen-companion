ALTER TABLE telegram_chat_db
DROP COLUMN ogs_account_name;

ALTER TABLE telegram_chat_db
DROP COLUMN ogs_account_id;

ALTER TABLE telegram_chat_db
DROP COLUMN email;


CREATE TABLE user_info_db (
    id                  UUID PRIMARY KEY,
    chat_id             BIGINT NOT NULL UNIQUE
                            REFERENCES telegram_chat_db(id),
    ogs_account_name    VARCHAR(255),
    ogs_account_id      BIGINT,
    email_address               VARCHAR(255)
);
