package io.github.davigdomingues.falainteligente.application.service;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.port.AiInteractionAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço responsável por gravar a trilha de auditoria das interações com IA em transação própria.
 *
 * Estrutura e impacto: utiliza {@link Propagation#REQUIRES_NEW} para assegurar que falhas no
 * provedor de IA, áudio inválido ou erros de validação ainda sejam persistidos como {@code FAILED}
 * para diagnóstico posterior e cálculo fiel da taxa de sucesso em {@code /api/v1/audit/metrics}.
 */
@Service
public class AiAuditRecorder {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_TRANSCRIPT_LENGTH = 2000;
    private static final int MAX_RESPONSE_LENGTH = 4000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final AiInteractionAuditRepository auditRepository;
    private final Clock clock;

    public AiAuditRecorder(AiInteractionAuditRepository auditRepository, Clock clock) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiInteractionAudit record(
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
            long totalLatencyMs
    ) {
        AiInteractionAudit audit = new AiInteractionAudit(
                UUID.randomUUID(),
                channel,
                truncate(originalFilename, MAX_FILENAME_LENGTH),
                audioSizeBytes,
                truncate(transcribedText, MAX_TRANSCRIPT_LENGTH),
                truncate(assistantResponse, MAX_RESPONSE_LENGTH),
                toolsInvoked == null ? List.of() : List.copyOf(toolsInvoked),
                status,
                truncate(errorMessage, MAX_ERROR_LENGTH),
                Math.max(0L, transcriptionLatencyMs),
                Math.max(0L, reasoningLatencyMs),
                Math.max(0L, speechLatencyMs),
                Math.max(0L, totalLatencyMs),
                Instant.now(clock)
        );
        return auditRepository.save(audit);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
