CREATE TABLE IF NOT EXISTS chat_journal (
    message_index   BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type    VARCHAR(20) NOT NULL,
    content         LONGTEXT NOT NULL,
    tokens          INTEGER NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_journal_conversation_id (conversation_id)
);

CREATE TABLE IF NOT EXISTS chat_journal_checkpoint (
    conversation_id  VARCHAR(255) PRIMARY KEY,
    checkpoint_index BIGINT NOT NULL,
    summary          LONGTEXT NOT NULL,
    tokens           INTEGER NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
