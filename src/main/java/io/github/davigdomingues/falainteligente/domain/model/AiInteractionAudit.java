package io.github.davigdomingues.falainteligente.domain.model;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutável de auditoria para cada interação processada pelo pipeline de IA.
 *
 * Estrutura e impacto: captura entrada (arquivo/texto), texto transcrito, resposta final,
 * ferramentas {@code @Tool} acionadas, estado ({@code SUCCESS}/{@code FAILED}) e latências por
 * estágio (STT, raciocínio LLM e TTS) para rastreabilidade e observabilidade operacional.
 */
public record AiInteractionAudit(
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

    public AiInteractionAudit {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        toolsInvoked = toolsInvoked == null ? List.of() : List.copyOf(toolsInvoked);
        if (transcriptionLatencyMs < 0 || reasoningLatencyMs < 0 || speechLatencyMs < 0 || totalLatencyMs < 0) {
            throw new IllegalArgumentException("Latency values must not be negative");
        }
    }
}
