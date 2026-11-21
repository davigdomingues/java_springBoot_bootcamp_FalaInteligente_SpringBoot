package io.github.davigdomingues.falainteligente.application.output;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado consolidado da execução de um comando de voz ou texto pelo assistente financeiro.
 *
 * Estrutura e impacto: reúne o identificador de auditoria, texto transcrito, resposta da IA,
 * ferramentas invocadas, áudio sintetizado (quando solicitado) e telemetria detalhada de latência.
 */
public record AssistantCommandOutput(
        UUID interactionId,
        String transcribedText,
        String assistantResponse,
        List<String> toolsInvoked,
        byte[] audioBytes,
        long transcriptionLatencyMs,
        long reasoningLatencyMs,
        long speechLatencyMs,
        long totalLatencyMs,
        Instant createdAt
) {

    public AssistantCommandOutput {
        Objects.requireNonNull(interactionId, "interactionId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        toolsInvoked = toolsInvoked == null ? List.of() : List.copyOf(toolsInvoked);
        audioBytes = audioBytes == null ? null : audioBytes.clone();
    }

    @Override
    public byte[] audioBytes() {
        return audioBytes == null ? null : audioBytes.clone();
    }
}
