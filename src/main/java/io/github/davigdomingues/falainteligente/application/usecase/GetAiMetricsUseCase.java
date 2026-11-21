package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.model.AiMetricsSummary;
import io.github.davigdomingues.falainteligente.domain.port.AiInteractionAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso para consolidação de métricas operacionais de IA (taxa de sucesso, latências médias
 * por estágio e ranking de ferramentas acionadas).
 */
@Service
public class GetAiMetricsUseCase {

    private final AiInteractionAuditRepository auditRepository;

    public GetAiMetricsUseCase(AiInteractionAuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
    }

    @Transactional(readOnly = true)
    public AiMetricsSummary execute() {
        List<AiInteractionAudit> audits = auditRepository.findAll(null, null);
        if (audits.isEmpty()) {
            return new AiMetricsSummary(
                    0L,
                    0L,
                    0L,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN),
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Map.of(),
                    Map.of()
            );
        }

        long total = audits.size();
        long successful = 0L;
        long failed = 0L;
        long sumTranscription = 0L;
        long sumReasoning = 0L;
        long sumSpeech = 0L;
        long sumTotal = 0L;
        Map<InteractionChannel, Long> byChannel = new EnumMap<>(InteractionChannel.class);
        Map<String, Long> toolCounts = new HashMap<>();

        for (AiInteractionAudit audit : audits) {
            if (audit.status() == InteractionStatus.SUCCESS) {
                successful++;
            } else {
                failed++;
            }
            sumTranscription += audit.transcriptionLatencyMs();
            sumReasoning += audit.reasoningLatencyMs();
            sumSpeech += audit.speechLatencyMs();
            sumTotal += audit.totalLatencyMs();
            byChannel.merge(audit.channel(), 1L, Long::sum);
            for (String tool : audit.toolsInvoked()) {
                toolCounts.merge(tool, 1L, Long::sum);
            }
        }

        BigDecimal successRate = BigDecimal.valueOf(successful)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN);

        Map<String, Long> sortedTools = new LinkedHashMap<>();
        toolCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEachOrdered(entry -> sortedTools.put(entry.getKey(), entry.getValue()));

        return new AiMetricsSummary(
                total,
                successful,
                failed,
                successRate,
                roundToTwoDecimals((double) sumTranscription / total),
                roundToTwoDecimals((double) sumReasoning / total),
                roundToTwoDecimals((double) sumSpeech / total),
                roundToTwoDecimals((double) sumTotal / total),
                byChannel,
                sortedTools
        );
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }
}
