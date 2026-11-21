package io.github.davigdomingues.falainteligente.domain.port;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência e consulta da trilha de auditoria das interações de IA.
 *
 * Estrutura e impacto: viabiliza rastreabilidade completa de comandos de voz, texto, STT e TTS
 * sem vazar detalhes de entidades JPA para a camada de aplicação.
 */
public interface AiInteractionAuditRepository {

    AiInteractionAudit save(AiInteractionAudit audit);

    Optional<AiInteractionAudit> findById(UUID id);

    List<AiInteractionAudit> findAll(InteractionStatus status, InteractionChannel channel);
}
