package io.github.davigdomingues.falainteligente.application;

import io.github.davigdomingues.falainteligente.application.input.CreateTransactionInput;
import io.github.davigdomingues.falainteligente.application.input.TransactionFilter;
import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.application.usecase.CreateTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.DeleteTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetCategoryBreakdownUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetFinancialSummaryUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetMonthlyReportUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ListTransactionsUseCase;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;
import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;
import io.github.davigdomingues.falainteligente.domain.model.MonthlyReport;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties.TransactionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários dos casos de uso transacionais e analíticos utilizando repositório em memória.
 *
 * Cobertura crítica: CRUD completo, filtragem por período/categoria, consolidação de saldo e
 * ticket médio, cálculo percentual por categoria, relatório mensal e validação de limites configuráveis.
 */
class TransactionUseCasesTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);

    private InMemoryTransactionRepository repository;
    private CreateTransactionUseCase createUseCase;
    private ListTransactionsUseCase listUseCase;
    private GetTransactionUseCase getUseCase;
    private DeleteTransactionUseCase deleteUseCase;
    private GetFinancialSummaryUseCase summaryUseCase;
    private GetCategoryBreakdownUseCase breakdownUseCase;
    private GetMonthlyReportUseCase monthlyReportUseCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        FalaInteligenteProperties properties = new FalaInteligenteProperties(
                null,
                new TransactionProperties(new BigDecimal("50000.00"), 120)
        );
        createUseCase = new CreateTransactionUseCase(repository, properties, FIXED_CLOCK);
        listUseCase = new ListTransactionsUseCase(repository);
        getUseCase = new GetTransactionUseCase(repository);
        deleteUseCase = new DeleteTransactionUseCase(repository);
        summaryUseCase = new GetFinancialSummaryUseCase(repository);
        breakdownUseCase = new GetCategoryBreakdownUseCase(repository);
        monthlyReportUseCase = new GetMonthlyReportUseCase(summaryUseCase, breakdownUseCase, repository);
    }

    @Test
    void shouldCreateListCalculateSummaryBreakdownAndMonthlyReport() {
        TransactionOutput salary = createUseCase.execute(new CreateTransactionInput(
                "Salário mensal",
                new BigDecimal("5000.00"),
                TransactionType.INCOME,
                Category.SALARY,
                LocalDate.of(2026, 11, 5),
                TransactionSource.REST
        ));
        TransactionOutput groceries = createUseCase.execute(new CreateTransactionInput(
                "Supermercado",
                new BigDecimal("300.00"),
                TransactionType.EXPENSE,
                Category.GROCERIES,
                LocalDate.of(2026, 11, 10),
                TransactionSource.VOICE_AI
        ));
        createUseCase.execute(new CreateTransactionInput(
                "Farmácia",
                new BigDecimal("100.00"),
                TransactionType.EXPENSE,
                Category.PHARMA,
                LocalDate.of(2026, 11, 15),
                TransactionSource.TEXT_AI
        ));
        createUseCase.execute(new CreateTransactionInput(
                "Viagem mês anterior",
                new BigDecimal("900.00"),
                TransactionType.EXPENSE,
                Category.LEISURE,
                LocalDate.of(2026, 10, 20),
                TransactionSource.REST
        ));

        assertThat(getUseCase.execute(salary.id().toString()).description()).isEqualTo("Salário mensal");
        assertThat(listUseCase.execute(new TransactionFilter(Category.GROCERIES, null, null, null)))
                .hasSize(1);
        assertThat(listUseCase.listRecent(2)).hasSize(2);

        FinancialSummary novSummary = summaryUseCase.execute(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(novSummary.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(novSummary.totalExpenses()).isEqualByComparingTo("400.00");
        assertThat(novSummary.balance()).isEqualByComparingTo("4600.00");
        assertThat(novSummary.averageExpense()).isEqualByComparingTo("200.00");
        assertThat(novSummary.totalCount()).isEqualTo(3);

        List<CategoryBreakdown> breakdown = breakdownUseCase.execute(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(breakdown).hasSize(2);
        assertThat(breakdown.get(0).category()).isEqualTo(Category.GROCERIES);
        assertThat(breakdown.get(0).percentageOfTotal()).isEqualByComparingTo("75.00");
        assertThat(breakdown.get(1).category()).isEqualTo(Category.PHARMA);
        assertThat(breakdown.get(1).percentageOfTotal()).isEqualByComparingTo("25.00");

        MonthlyReport report = monthlyReportUseCase.execute(2026, 11);
        assertThat(report.summary().balance()).isEqualByComparingTo("4600.00");
        assertThat(report.expensesByCategory()).hasSize(2);
        assertThat(report.recentTransactions()).hasSize(3);

        TransactionOutput deleted = deleteUseCase.execute(groceries.id().toString());
        assertThat(deleted.id()).isEqualTo(groceries.id());
        assertThatThrownBy(() -> getUseCase.execute(groceries.id().toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldValidateConfiguredCeilingAndFilterRanges() {
        assertThatThrownBy(() -> createUseCase.execute(new CreateTransactionInput(
                "Compra acima do teto configurado",
                new BigDecimal("60000.00"),
                TransactionType.EXPENSE,
                Category.OTHER,
                LocalDate.of(2026, 11, 20),
                TransactionSource.REST
        ))).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> new TransactionFilter(
                null, null, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10)))
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> monthlyReportUseCase.execute(2026, 13))
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> deleteUseCase.execute(UUID.randomUUID().toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {

        private final Map<TransactionId, Transaction> storage = new LinkedHashMap<>();

        @Override
        public Transaction save(Transaction transaction) {
            storage.put(transaction.id(), transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findById(TransactionId id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Transaction> findAll(
                Category category, TransactionType type, LocalDate startDate, LocalDate endDate) {
            return storage.values().stream()
                    .filter(tx -> category == null || tx.category() == category)
                    .filter(tx -> type == null || tx.type() == type)
                    .filter(tx -> startDate == null || !tx.transactionDate().isBefore(startDate))
                    .filter(tx -> endDate == null || !tx.transactionDate().isAfter(endDate))
                    .sorted(Comparator.comparing(Transaction::transactionDate)
                            .thenComparing(Transaction::createdAt).reversed())
                    .toList();
        }

        @Override
        public List<Transaction> findRecent(int limit) {
            return findAll(null, null, null, null).stream().limit(limit).toList();
        }

        @Override
        public void deleteById(TransactionId id) {
            storage.remove(id);
        }

        @Override
        public boolean existsById(TransactionId id) {
            return storage.containsKey(id);
        }
    }
}
