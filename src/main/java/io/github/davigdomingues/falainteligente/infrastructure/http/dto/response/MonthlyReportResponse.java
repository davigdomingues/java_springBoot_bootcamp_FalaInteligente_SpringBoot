package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.domain.model.MonthlyReport;

import java.util.List;

/**
 * Resposta JSON do relatório mensal consolidado.
 */
public record MonthlyReportResponse(
        int year,
        int month,
        FinancialSummaryResponse summary,
        List<CategoryBreakdownResponse> expensesByCategory,
        List<TransactionResponse> transactions
) {

    public static MonthlyReportResponse from(MonthlyReport report) {
        return new MonthlyReportResponse(
                report.year(),
                report.month(),
                FinancialSummaryResponse.from(report.summary()),
                report.expensesByCategory().stream().map(CategoryBreakdownResponse::from).toList(),
                report.recentTransactions().stream().map(TransactionResponse::from).toList()
        );
    }
}
