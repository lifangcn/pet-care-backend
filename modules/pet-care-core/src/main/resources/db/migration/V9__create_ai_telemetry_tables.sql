CREATE TABLE petcare.chat_trace (
    trace_id uuid PRIMARY KEY,
    conversation_id text,
    session_id text,
    user_id bigint,
    occurred_at timestamptz NOT NULL,
    duration_ms integer CONSTRAINT ck_chat_trace_duration_ms CHECK (duration_ms >= 0),
    request_data jsonb NOT NULL DEFAULT '{}'::jsonb,
    response_data jsonb NOT NULL DEFAULT '{}'::jsonb,
    rag_data jsonb,
    tool_calls jsonb NOT NULL DEFAULT '[]'::jsonb,
    error_data jsonb,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    expires_at timestamptz NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
);

CREATE TABLE petcare.agent_execution (
    execution_id uuid PRIMARY KEY,
    agent_type text NOT NULL,
    conversation_id text,
    user_id bigint,
    query text NOT NULL,
    steps jsonb NOT NULL DEFAULT '[]'::jsonb,
    final_answer text,
    success boolean NOT NULL,
    reason text,
    total_steps integer NOT NULL CONSTRAINT ck_agent_execution_total_steps CHECK (total_steps >= 0),
    tool_calls integer NOT NULL CONSTRAINT ck_agent_execution_tool_calls CHECK (tool_calls >= 0),
    total_duration_ms bigint NOT NULL CONSTRAINT ck_agent_execution_total_duration_ms CHECK (total_duration_ms >= 0),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
);

CREATE INDEX idx_chat_trace_expires_at ON petcare.chat_trace (expires_at);
CREATE INDEX idx_agent_execution_expires_at ON petcare.agent_execution (expires_at);

GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.chat_trace TO petcare_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.agent_execution TO petcare_app;
