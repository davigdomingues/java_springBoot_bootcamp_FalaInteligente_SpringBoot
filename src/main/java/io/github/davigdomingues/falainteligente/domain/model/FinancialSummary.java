package io.github.davigdomingues.falainteligente.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Agregado consolidado de receitas, despesas, saldo líquido e ticket médio de despesas.
 *
 * Estrutura e impacto: modelo analítico imutável utilizado tanto pelo endpoint REST de resumo
 * quanto pela ferramenta {@code get-financial-summary} do assistente de IA.
 */
public record FinancialSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        long incomeCount,
        long expenseCount,
        long totalCount,
        BigDecimal averageExpense,
        LocalDate startDate,
        LocalDate endDate
) {

    public FinancialSummary {
        Objects.requireNonNull(totalIncome, "totalIncome must not be null");
        Objects.requireNonNull(totalExpenses, "totalExpenses must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(averageExpense, "averageExpense must not be null");
    }
}
