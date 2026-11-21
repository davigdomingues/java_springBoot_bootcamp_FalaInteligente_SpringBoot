package io.github.davigdomingues.falainteligente.application.output;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Visão de saída de uma transação financeira para consumo pela API HTTP e ferramentas de IA.
 */
public record TransactionOutput(
        UUID id,
        String description,
        BigDecimal amount,
        TransactionType type,
        Category category,
        String categoryDisplayName,
        LocalDate transactionDate,
        TransactionSource source,
        Instant createdAt
) {

    public static TransactionOutput from(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        return new TransactionOutput(
                transaction.id().value(),
                transaction.description(),
                transaction.amount(),
                transaction.type(),
                transaction.category(),
                transaction.category().getDisplayNamePtBr(),
                transaction.transactionDate(),
                transaction.source(),
                transaction.createdAt()
        );
    }
}
