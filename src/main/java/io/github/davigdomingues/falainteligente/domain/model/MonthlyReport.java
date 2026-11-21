package io.github.davigdomingues.falainteligente.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Relatório financeiro consolidado para um mês e ano específicos.
 *
 * Estrutura e impacto: combina resumo contábil do período, ranking de despesas por categoria e
 * transações ordenadas para visão gerencial em uma única consulta REST.
 */
public record MonthlyReport(
        int year,
        int month,
        FinancialSummary summary,
        List<CategoryBreakdown> expensesByCategory,
        List<Transaction> recentTransactions
) {

    public MonthlyReport {
        Objects.requireNonNull(summary, "summary must not be null");
        expensesByCategory = expensesByCategory == null ? List.of() : List.copyOf(expensesByCategory);
        recentTransactions = recentTransactions == null ? List.of() : List.copyOf(recentTransactions);
    }
}
