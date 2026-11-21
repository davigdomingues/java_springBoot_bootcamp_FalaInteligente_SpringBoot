package io.github.davigdomingues.falainteligente.domain.model;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidade raiz imutável representando uma movimentação financeira no orçamento pessoal.
 *
 * Estrutura e impacto: garante invariantes contábeis e semânticas diretamente no construtor
 * compacto - normalizando {@code amount} com escala monetária de 2 casas ({@link RoundingMode#HALF_EVEN}),
 * validando limites positivos e higienizando a descrição - independentemente da camada que a construa
 * (REST, Tool Calling ou reconstituição JPA).
 */
public record Transaction(
        TransactionId id,
        String description,
        BigDecimal amount,
        TransactionType type,
        Category category,
        LocalDate transactionDate,
        TransactionSource source,
        Instant createdAt
) {

    public static final int MIN_DESCRIPTION_LENGTH = 2;
    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("1000000.00");

    public Transaction {
        if (id == null) {
            throw new DomainValidationException("Transaction id must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new DomainValidationException("Transaction description must not be blank");
        }
        description = description.trim();
        if (description.length() < MIN_DESCRIPTION_LENGTH || description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DomainValidationException(
                    "Transaction description must have between "
                            + MIN_DESCRIPTION_LENGTH + " and " + MAX_DESCRIPTION_LENGTH + " characters"
            );
        }
        if (amount == null) {
            throw new DomainValidationException("Transaction amount must not be null");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Transaction amount must be greater than zero");
        }
        if (amount.compareTo(DEFAULT_MAX_AMOUNT) > 0) {
            throw new DomainValidationException(
                    "Transaction amount exceeds maximum allowed value of " + DEFAULT_MAX_AMOUNT
            );
        }
        if (type == null) {
            throw new DomainValidationException("Transaction type must not be null");
        }
        if (category == null) {
            throw new DomainValidationException("Transaction category must not be null");
        }
        if (transactionDate == null) {
            throw new DomainValidationException("Transaction date must not be null");
        }
        if (source == null) {
            throw new DomainValidationException("Transaction source must not be null");
        }
        if (createdAt == null) {
            throw new DomainValidationException("Transaction createdAt must not be null");
        }
    }

    /**
     * Cria uma nova transação com identificador gerado e timestamp ancorado no {@link Clock} informado.
     */
    public static Transaction create(
            String description,
            BigDecimal amount,
            TransactionType type,
            Category category,
            LocalDate transactionDate,
            TransactionSource source,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock must not be null");
        LocalDate effectiveDate = transactionDate != null ? transactionDate : LocalDate.now(clock);
        return new Transaction(
                TransactionId.generate(),
                description,
                amount,
                type,
                category,
                effectiveDate,
                source,
                Instant.now(clock)
        );
    }
}
