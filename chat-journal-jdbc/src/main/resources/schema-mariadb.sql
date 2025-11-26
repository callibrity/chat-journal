CREATE TABLE IF NOT EXISTS chat_journal (
    message_index   BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type    VARCHAR(20) NOT NULL,
    content         LONGTEXT NOT NULL,
    tokens          INTEGER NOT NULL,
    INDEX idx_chat_journal_conversation_id (conversation_id)
);
