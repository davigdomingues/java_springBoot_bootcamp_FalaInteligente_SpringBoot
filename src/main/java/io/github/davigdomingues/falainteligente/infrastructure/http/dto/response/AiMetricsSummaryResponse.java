package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.model.AiMetricsSummary;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resposta JSON com métricas consolidadas do pipeline de Inteligência Artificial.
 */
public record AiMetricsSummaryResponse(
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

    public static AiMetricsSummaryResponse from(AiMetricsSummary summary) {
        return new AiMetricsSummaryResponse(
                summary.totalInteractions(),
                summary.successfulInteractions(),
                summary.failedInteractions(),
                summary.successRatePercent(),
                summary.avgTranscriptionLatencyMs(),
                summary.avgReasoningLatencyMs(),
                summary.avgSpeechLatencyMs(),
                summary.avgTotalLatencyMs(),
                summary.interactionsByChannel(),
                summary.toolUsageCounts()
        );
    }
}
