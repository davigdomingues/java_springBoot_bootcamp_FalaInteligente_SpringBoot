package io.github.davigdomingues.falainteligente.infrastructure.persistence.repository;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import io.github.davigdomingues.falainteligente.infrastructure.persistence.entity.TransactionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de saída JPA que implementa a porta de domínio {@link TransactionRepository}.
 *
 * Estrutura e impacto: utiliza JPA {@link Specification} para montar predicados apenas sobre
 * filtros informados, evitando ambiguidades de tipagem de parâmetros nulos entre H2 e PostgreSQL.
 */
@Repository
public class JpaTransactionRepositoryAdapter implements TransactionRepository {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("transactionDate"),
            Sort.Order.desc("createdAt")
    );

    private final SpringDataTransactionRepository springDataRepository;

    public JpaTransactionRepositoryAdapter(SpringDataTransactionRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(
                springDataRepository, "springDataRepository must not be null");
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = springDataRepository.save(TransactionEntity.fromDomain(transaction));
        return saved.toDomain();
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        Objects.requireNonNull(id, "id must not be null");
        return springDataRepository.findById(id.value().toString())
                .map(TransactionEntity::toDomain);
    }

    @Override
    public List<Transaction> findAll(
            Category category,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Specification<TransactionEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return springDataRepository.findAll(specification, DEFAULT_SORT)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findRecent(int limit) {
        return springDataRepository.findAll(PageRequest.of(0, limit, DEFAULT_SORT))
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(TransactionId id) {
        Objects.requireNonNull(id, "id must not be null");
        springDataRepository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsById(TransactionId id) {
        Objects.requireNonNull(id, "id must not be null");
        return springDataRepository.existsById(id.value().toString());
    }
}
