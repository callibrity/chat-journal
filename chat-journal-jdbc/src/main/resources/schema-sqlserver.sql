CREATE TABLE chat_journal (
    message_index   BIGINT IDENTITY(1,1) PRIMARY KEY,
    conversation_id NVARCHAR(255) NOT NULL,
    message_type    NVARCHAR(20) NOT NULL,
    content         NVARCHAR(MAX) NOT NULL,
    tokens          INT NOT NULL
);

CREATE INDEX idx_chat_journal_conversation_id ON chat_journal (conversation_id);
