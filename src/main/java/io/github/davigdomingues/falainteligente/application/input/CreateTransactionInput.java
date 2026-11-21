package io.github.davigdomingues.falainteligente.application.input;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comando de entrada imutável para criação de uma nova transação financeira.
 *
 * Estrutura e impacto: compartilhado entre os controladores REST e as ferramentas de Tool Calling
 * da IA, unificando as validações de negócio no caso de uso {@code CreateTransactionUseCase}.
 */
public record CreateTransactionInput(
        String description,
        BigDecimal amount,
        TransactionType type,
        Category category,
        LocalDate transactionDate,
        TransactionSource source
) {

    public CreateTransactionInput {
        source = source != null ? source : TransactionSource.REST;
    }
}
