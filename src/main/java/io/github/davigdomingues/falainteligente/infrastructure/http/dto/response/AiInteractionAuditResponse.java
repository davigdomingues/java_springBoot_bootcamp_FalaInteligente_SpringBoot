package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.application.output.AiInteractionAuditOutput;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta JSON de um registro de auditoria das interações com IA.
 */
public record AiInteractionAuditResponse(
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

    public static AiInteractionAuditResponse from(AiInteractionAuditOutput output) {
        return new AiInteractionAuditResponse(
                output.id(),
                output.channel(),
                output.originalFilename(),
                output.audioSizeBytes(),
                output.transcribedText(),
                output.assistantResponse(),
                output.toolsInvoked(),
                output.status(),
                output.errorMessage(),
                output.transcriptionLatencyMs(),
                output.reasoningLatencyMs(),
                output.speechLatencyMs(),
                output.totalLatencyMs(),
                output.createdAt()
        );
    }
}
