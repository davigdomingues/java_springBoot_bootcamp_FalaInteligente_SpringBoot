package io.github.davigdomingues.falainteligente.application.output;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Visão de saída imutável dos registros de auditoria de interações com IA.
 */
public record AiInteractionAuditOutput(
        UUID id,
        InteractionChannel channel,
        String originalFilename,
        Long audioSizeBytes,
        String transcribedText,
        String assistantResponse,
        List<String> toolsInvoked,
        InteractionStatus status,
        String errorMessage,
        long transcriptionLatencyMs,
        long reasoningLatencyMs,
        long speechLatencyMs,
        long totalLatencyMs,
        Instant createdAt
) {

    public static AiInteractionAuditOutput from(AiInteractionAudit audit) {
        Objects.requireNonNull(audit, "audit must not be null");
        return new AiInteractionAuditOutput(
                audit.id(),
                audit.channel(),
                audit.originalFilename(),
                audit.audioSizeBytes(),
                audit.transcribedText(),
                audit.assistantResponse(),
                audit.toolsInvoked(),
                audit.status(),
                audit.errorMessage(),
                audit.transcriptionLatencyMs(),
                audit.reasoningLatencyMs(),
                audit.speechLatencyMs(),
                audit.totalLatencyMs(),
                audit.createdAt()
        );
    }
}
