CREATE TABLE IF NOT EXISTS chat_memory
(
    conversation_id   VARCHAR(100) NOT NULL,
    id                BIGINT       NOT NULL,
    role              VARCHAR(10)  NOT NULL,
    content           TEXT         NOT NULL,
    reasoning_content TEXT         NOT NULL,
    tool_calls        TEXT,
    tool_call_id      VARCHAR(50),
    PRIMARY KEY (conversation_id, id)
);