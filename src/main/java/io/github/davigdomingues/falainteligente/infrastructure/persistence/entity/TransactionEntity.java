package io.github.davigdomingues.falainteligente.infrastructure.persistence.entity;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA correspondente à tabela {@code transactions} gerenciada via Flyway.
 *
 * Estrutura e impacto: permanece isolada na camada de infraestrutura, convertendo explicitamente
 * entre o modelo de domínio {@link Transaction} e o esquema relacional compatível com H2 e PostgreSQL.
 */
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private TransactionSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransactionEntity() {
    }

    public static TransactionEntity fromDomain(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        TransactionEntity entity = new TransactionEntity();
        entity.id = transaction.id().value().toString();
        entity.description = transaction.description();
        entity.amount = transaction.amount();
        entity.type = transaction.type();
        entity.category = transaction.category();
        entity.transactionDate = transaction.transactionDate();
        entity.source = transaction.source();
        entity.createdAt = transaction.createdAt();
        return entity;
    }

    public Transaction toDomain() {
        return new Transaction(
                new TransactionId(UUID.fromString(id)),
                description,
                amount,
                type,
                category,
                transactionDate,
                source,
                createdAt
        );
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public TransactionSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
