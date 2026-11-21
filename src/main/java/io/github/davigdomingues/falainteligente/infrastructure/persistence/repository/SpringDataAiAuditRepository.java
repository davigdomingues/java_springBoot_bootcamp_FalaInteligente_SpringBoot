package io.github.davigdomingues.falainteligente.infrastructure.persistence.repository;

import io.github.davigdomingues.falainteligente.infrastructure.persistence.entity.AiInteractionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositório Spring Data JPA para {@link AiInteractionAuditEntity} com filtros dinâmicos.
 */
public interface SpringDataAiAuditRepository
        extends JpaRepository<AiInteractionAuditEntity, String>, JpaSpecificationExecutor<AiInteractionAuditEntity> {
}
