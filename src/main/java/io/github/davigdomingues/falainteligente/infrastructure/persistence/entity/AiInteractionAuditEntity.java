package io.github.davigdomingues.falainteligente.infrastructure.persistence.entity;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA correspondente à tabela {@code ai_interaction_audit_logs}.
 *
 * Estrutura e impacto: persiste a trilha completa das interações de voz, texto, transcrição e
 * síntese de fala, serializando a lista de ferramentas acionadas de forma portável entre H2 e PostgreSQL.
 */
@Entity
@Table(name = "ai_interaction_audit_logs")
public class AiInteractionAuditEntity {

    private static final int MAX_TOOLS_COLUMN_LENGTH = 500;

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private InteractionChannel channel;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "audio_size_bytes")
    private Long audioSizeBytes;

    @Column(name = "transcribed_text", length = 2000)
    private String transcribedText;

    @Column(name = "assistant_response", length = 4000)
    private String assistantResponse;

    @Column(name = "tools_invoked", length = 500)
    private String toolsInvoked;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InteractionStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "transcription_latency_ms", nullable = false)
    private long transcriptionLatencyMs;

    @Column(name = "reasoning_latency_ms", nullable = false)
    private long reasoningLatencyMs;

    @Column(name = "speech_latency_ms", nullable = false)
    private long speechLatencyMs;

    @Column(name = "total_latency_ms", nullable = false)
    private long totalLatencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiInteractionAuditEntity() {
    }

    public static AiInteractionAuditEntity fromDomain(AiInteractionAudit audit) {
        Objects.requireNonNull(audit, "audit must not be null");
        AiInteractionAuditEntity entity = new AiInteractionAuditEntity();
        entity.id = audit.id().toString();
        entity.channel = audit.channel();
        entity.originalFilename = audit.originalFilename();
        entity.audioSizeBytes = audit.audioSizeBytes();
        entity.transcribedText = audit.transcribedText();
        entity.assistantResponse = audit.assistantResponse();
        entity.toolsInvoked = serializeTools(audit.toolsInvoked());
        entity.status = audit.status();
        entity.errorMessage = audit.errorMessage();
        entity.transcriptionLatencyMs = audit.transcriptionLatencyMs();
        entity.reasoningLatencyMs = audit.reasoningLatencyMs();
        entity.speechLatencyMs = audit.speechLatencyMs();
        entity.totalLatencyMs = audit.totalLatencyMs();
        entity.createdAt = audit.createdAt();
        return entity;
    }

    public AiInteractionAudit toDomain() {
        return new AiInteractionAudit(
                UUID.fromString(id),
                channel,
                originalFilename,
                audioSizeBytes,
                transcribedText,
                assistantResponse,
                deserializeTools(toolsInvoked),
                status,
                errorMessage,
                transcriptionLatencyMs,
                reasoningLatencyMs,
                speechLatencyMs,
                totalLatencyMs,
                createdAt
        );
    }

    private static String serializeTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return "";
        }
        String joined = String.join(",", tools);
        return joined.length() <= MAX_TOOLS_COLUMN_LENGTH
                ? joined
                : joined.substring(0, MAX_TOOLS_COLUMN_LENGTH);
    }

    private static List<String> deserializeTools(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    public String getId() {
        return id;
    }

    public InteractionChannel getChannel() {
        return channel;
    }

    public InteractionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
