package io.github.davigdomingues.falainteligente.infrastructure.persistence.repository;

import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.port.AiInteractionAuditRepository;
import io.github.davigdomingues.falainteligente.infrastructure.persistence.entity.AiInteractionAuditEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de saída JPA que implementa {@link AiInteractionAuditRepository}.
 */
@Repository
public class JpaAiAuditRepositoryAdapter implements AiInteractionAuditRepository {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

    private final SpringDataAiAuditRepository springDataRepository;

    public JpaAiAuditRepositoryAdapter(SpringDataAiAuditRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(
                springDataRepository, "springDataRepository must not be null");
    }

    @Override
    public AiInteractionAudit save(AiInteractionAudit audit) {
        AiInteractionAuditEntity saved = springDataRepository.save(AiInteractionAuditEntity.fromDomain(audit));
        return saved.toDomain();
    }

    @Override
    public Optional<AiInteractionAudit> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return springDataRepository.findById(id.toString())
                .map(AiInteractionAuditEntity::toDomain);
    }

    @Override
    public List<AiInteractionAudit> findAll(InteractionStatus status, InteractionChannel channel) {
        Specification<AiInteractionAuditEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (channel != null) {
                predicates.add(criteriaBuilder.equal(root.get("channel"), channel));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return springDataRepository.findAll(specification, DEFAULT_SORT)
                .stream()
                .map(AiInteractionAuditEntity::toDomain)
                .toList();
    }
}
