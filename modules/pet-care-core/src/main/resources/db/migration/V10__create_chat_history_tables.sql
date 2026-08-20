CREATE TABLE petcare.chat_session (
    user_id bigint NOT NULL,
    session_id varchar(32) NOT NULL,
    name varchar(100) NOT NULL DEFAULT '新对话',
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT pk_chat_session PRIMARY KEY (user_id, session_id),
    CONSTRAINT ck_chat_session_name CHECK (char_length(btrim(name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_chat_session_expires_at CHECK (expires_at >= created_at)
);

CREATE TABLE petcare.chat_message (
    id bigint PRIMARY KEY,
    user_id bigint NOT NULL,
    session_id varchar(32) NOT NULL,
    conversation_id varchar(128) NOT NULL,
    role varchar(16) NOT NULL,
    content text NOT NULL,
    embedding petcare.vector(1024),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (user_id, session_id)
        REFERENCES petcare.chat_session (user_id, session_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_chat_message_metadata_object CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT ck_chat_message_expires_at CHECK (expires_at >= created_at)
);

CREATE INDEX idx_chat_session_user_updated_at_session_id
    ON petcare.chat_session (user_id, updated_at DESC, session_id);
CREATE INDEX idx_chat_session_expires_at ON petcare.chat_session (expires_at);
CREATE INDEX idx_chat_message_owner_session_created_at_id
    ON petcare.chat_message (user_id, session_id, created_at, id);
CREATE INDEX idx_chat_message_user_created_at ON petcare.chat_message (user_id, created_at DESC);
CREATE INDEX idx_chat_message_expires_at ON petcare.chat_message (expires_at);
CREATE INDEX idx_chat_message_embedding_hnsw
    ON petcare.chat_message USING hnsw (embedding petcare.vector_cosine_ops)
    WHERE role = 'USER' AND embedding IS NOT NULL;

GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.chat_session TO petcare_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.chat_message TO petcare_app;
