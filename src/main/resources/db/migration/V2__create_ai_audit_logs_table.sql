CREATE TABLE ai_interaction_audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    channel VARCHAR(20) NOT NULL,
    original_filename VARCHAR(255),
    audio_size_bytes BIGINT,
    transcribed_text VARCHAR(2000),
    assistant_response VARCHAR(4000),
    tools_invoked VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    transcription_latency_ms BIGINT NOT NULL,
    reasoning_latency_ms BIGINT NOT NULL,
    speech_latency_ms BIGINT NOT NULL,
    total_latency_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_audit_created_at ON ai_interaction_audit_logs (created_at);
CREATE INDEX idx_ai_audit_status ON ai_interaction_audit_logs (status);
