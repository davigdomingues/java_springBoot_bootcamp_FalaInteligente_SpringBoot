package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso analítico que consolida receitas, despesas, saldo líquido e ticket médio em uma
 * janela temporal opcional.
 */
@Service
public class GetFinancialSummaryUseCase {

    private final TransactionRepository transactionRepository;

    public GetFinancialSummaryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Transactional(readOnly = true)
    public FinancialSummary execute(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainValidationException("startDate must not be after endDate");
        }

        List<Transaction> transactions = transactionRepository.findAll(null, null, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalExpenses = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        long incomeCount = 0;
        long expenseCount = 0;

        for (Transaction transaction : transactions) {
            if (transaction.type() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(transaction.amount());
                incomeCount++;
            } else {
                totalExpenses = totalExpenses.add(transaction.amount());
                expenseCount++;
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal averageExpense = expenseCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : totalExpenses.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_EVEN);

        return new FinancialSummary(
                totalIncome.setScale(2, RoundingMode.HALF_EVEN),
                totalExpenses.setScale(2, RoundingMode.HALF_EVEN),
                balance,
                incomeCount,
                expenseCount,
                incomeCount + expenseCount,
                averageExpense,
                startDate,
                endDate
        );
    }
}
