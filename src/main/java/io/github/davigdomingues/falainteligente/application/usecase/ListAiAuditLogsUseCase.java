package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.output.AiInteractionAuditOutput;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.port.AiInteractionAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso para listagem e consulta detalhada da trilha de auditoria das interações de IA.
 */
@Service
public class ListAiAuditLogsUseCase {

    private final AiInteractionAuditRepository auditRepository;

    public ListAiAuditLogsUseCase(AiInteractionAuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<AiInteractionAuditOutput> execute(InteractionStatus status, InteractionChannel channel) {
        return auditRepository.findAll(status, channel)
                .stream()
                .map(AiInteractionAuditOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiInteractionAuditOutput getById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return auditRepository.findById(id)
                .map(AiInteractionAuditOutput::from)
                .orElseThrow(() -> new ResourceNotFoundException("AI audit log not found: " + id));
    }
}
