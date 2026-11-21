package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso analítico que agrupa despesas por categoria e calcula a participação percentual
 * de cada categoria no total gasto.
 */
@Service
public class GetCategoryBreakdownUseCase {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;

    public GetCategoryBreakdownUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdown> execute(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainValidationException("startDate must not be after endDate");
        }

        List<Transaction> expenses = transactionRepository.findAll(null, TransactionType.EXPENSE, startDate, endDate);
        if (expenses.isEmpty()) {
            return List.of();
        }

        Map<Category, BigDecimal> totalsByCategory = new EnumMap<>(Category.class);
        Map<Category, Long> countsByCategory = new EnumMap<>(Category.class);
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Transaction expense : expenses) {
            totalsByCategory.merge(expense.category(), expense.amount(), BigDecimal::add);
            countsByCategory.merge(expense.category(), 1L, Long::sum);
            grandTotal = grandTotal.add(expense.amount());
        }

        BigDecimal totalExpenses = grandTotal.setScale(2, RoundingMode.HALF_EVEN);
        return totalsByCategory.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();
                    BigDecimal categoryTotal = entry.getValue().setScale(2, RoundingMode.HALF_EVEN);
                    long count = countsByCategory.getOrDefault(category, 0L);
                    BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                            : categoryTotal.multiply(ONE_HUNDRED).divide(totalExpenses, 2, RoundingMode.HALF_EVEN);
                    return new CategoryBreakdown(
                            category,
                            category.getDisplayNamePtBr(),
                            categoryTotal,
                            count,
                            percentage
                    );
                })
                .sorted(Comparator.comparing(CategoryBreakdown::totalAmount).reversed()
                        .thenComparing(item -> item.category().name()))
                .toList();
    }
}
