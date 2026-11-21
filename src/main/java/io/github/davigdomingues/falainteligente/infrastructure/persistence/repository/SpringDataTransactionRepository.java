package io.github.davigdomingues.falainteligente.infrastructure.persistence.repository;

import io.github.davigdomingues.falainteligente.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositório Spring Data JPA para {@link TransactionEntity} com suporte a especificações dinâmicas.
 */
public interface SpringDataTransactionRepository
        extends JpaRepository<TransactionEntity, String>, JpaSpecificationExecutor<TransactionEntity> {
}
