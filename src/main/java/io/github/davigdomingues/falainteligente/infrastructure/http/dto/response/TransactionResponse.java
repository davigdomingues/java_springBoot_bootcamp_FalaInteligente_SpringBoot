package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação JSON pública de uma transação financeira.
 */
public record TransactionResponse(
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

    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(
                output.id(),
                output.description(),
                output.amount(),
                output.type(),
                output.category(),
                output.categoryDisplayName(),
                output.transactionDate(),
                output.source(),
                output.createdAt()
        );
    }

    public static TransactionResponse from(Transaction transaction) {
        return from(TransactionOutput.from(transaction));
    }
}
