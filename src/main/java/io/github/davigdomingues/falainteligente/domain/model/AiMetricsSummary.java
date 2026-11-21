package io.github.davigdomingues.falainteligente.domain.model;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Consolidação das métricas operacionais e de latência das interações com IA.
 *
 * Estrutura e impacto: sintetiza taxa de sucesso, médias de latência por estágio (STT, LLM, TTS),
 * volume por canal e frequência de invocação de cada ferramenta {@code @Tool}.
 */
public record AiMetricsSummary(
        long totalInteractions,
        long successfulInteractions,
        long failedInteractions,
        BigDecimal successRatePercent,
        double avgTranscriptionLatencyMs,
        double avgReasoningLatencyMs,
        double avgSpeechLatencyMs,
        double avgTotalLatencyMs,
        Map<InteractionChannel, Long> interactionsByChannel,
        Map<String, Long> toolUsageCounts
) {

    public AiMetricsSummary {
        Objects.requireNonNull(successRatePercent, "successRatePercent must not be null");
        interactionsByChannel = interactionsByChannel == null ? Map.of() : Map.copyOf(interactionsByChannel);
        toolUsageCounts = toolUsageCounts == null ? Map.of() : Map.copyOf(toolUsageCounts);
    }
}
