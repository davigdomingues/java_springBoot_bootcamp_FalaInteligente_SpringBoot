package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta JSON consolidada de receitas, despesas, saldo e ticket médio.
 */
public record FinancialSummaryResponse(
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

    public static FinancialSummaryResponse from(FinancialSummary summary) {
        return new FinancialSummaryResponse(
                summary.totalIncome(),
                summary.totalExpenses(),
                summary.balance(),
                summary.incomeCount(),
                summary.expenseCount(),
                summary.totalCount(),
                summary.averageExpense(),
                summary.startDate(),
                summary.endDate()
        );
    }
}
