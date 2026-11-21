package io.github.davigdomingues.falainteligente.infrastructure.http.dto.request;

import io.github.davigdomingues.falainteligente.application.input.CreateTransactionInput;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload REST validado via Jakarta Bean Validation para criação manual de transações financeiras.
 */
public record CreateTransactionRequest(
        @Schema(description = "Descrição objetiva da transação", example = "Compras no supermercado")
        @NotBlank(message = "description must not be blank")
        @Size(min = 2, max = 200, message = "description must have between 2 and 200 characters")
        String description,

        @Schema(description = "Valor monetário positivo em reais", example = "85.50")
        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @DecimalMax(value = "1000000.00", message = "amount must not exceed 1000000.00")
        BigDecimal amount,

        @Schema(description = "Tipo contábil da transação", example = "EXPENSE")
        @NotNull(message = "type must not be null")
        TransactionType type,

        @Schema(description = "Categoria financeira", example = "GROCERIES")
        @NotNull(message = "category must not be null")
        Category category,

        @Schema(description = "Data da transação (ISO-8601); se omitida, assume a data atual UTC", example = "2026-11-20")
        LocalDate transactionDate
) {

    public CreateTransactionInput toInput() {
        return new CreateTransactionInput(
                description,
                amount,
                type,
                category,
                transactionDate,
                TransactionSource.REST
        );
    }
}
