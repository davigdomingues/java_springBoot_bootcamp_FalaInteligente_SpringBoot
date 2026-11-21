package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;
import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;
import io.github.davigdomingues.falainteligente.domain.model.MonthlyReport;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para geração do relatório financeiro mensal completo (resumo, ranking por categoria
 * e lançamentos do período).
 */
@Service
public class GetMonthlyReportUseCase {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final GetFinancialSummaryUseCase getFinancialSummaryUseCase;
    private final GetCategoryBreakdownUseCase getCategoryBreakdownUseCase;
    private final TransactionRepository transactionRepository;

    public GetMonthlyReportUseCase(
            GetFinancialSummaryUseCase getFinancialSummaryUseCase,
            GetCategoryBreakdownUseCase getCategoryBreakdownUseCase,
            TransactionRepository transactionRepository
    ) {
        this.getFinancialSummaryUseCase = Objects.requireNonNull(
                getFinancialSummaryUseCase, "getFinancialSummaryUseCase must not be null");
        this.getCategoryBreakdownUseCase = Objects.requireNonNull(
                getCategoryBreakdownUseCase, "getCategoryBreakdownUseCase must not be null");
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository, "transactionRepository must not be null");
    }

    @Transactional(readOnly = true)
    public MonthlyReport execute(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new DomainValidationException("Year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (month < 1 || month > 12) {
            throw new DomainValidationException("Month must be between 1 and 12");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        FinancialSummary summary = getFinancialSummaryUseCase.execute(startDate, endDate);
        List<CategoryBreakdown> breakdown = getCategoryBreakdownUseCase.execute(startDate, endDate);
        List<Transaction> transactions = transactionRepository.findAll(null, null, startDate, endDate);

        return new MonthlyReport(year, month, summary, breakdown, transactions);
    }
}
