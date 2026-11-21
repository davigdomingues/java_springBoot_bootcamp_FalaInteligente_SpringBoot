package io.github.davigdomingues.falainteligente.infrastructure.ai;

import io.github.davigdomingues.falainteligente.application.input.CreateTransactionInput;
import io.github.davigdomingues.falainteligente.application.input.TransactionFilter;
import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.application.service.ToolInvocationContext;
import io.github.davigdomingues.falainteligente.application.usecase.CreateTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.DeleteTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetCategoryBreakdownUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetFinancialSummaryUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ListTransactionsUseCase;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;
import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;
import io.github.davigdomingues.falainteligente.infrastructure.ai.tool.FinancialAssistantTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do catálogo de ferramentas {@link FinancialAssistantTools}.
 *
 * Cobertura crítica: delegação das 6 {@code @Tool}s aos casos de uso, propagação de
 * {@link TransactionSource} via {@link ToolInvocationContext}, clamping de limites e validação de datas.
 */
@ExtendWith(MockitoExtension.class)
class FinancialAssistantToolsTest {

    @Mock
    private CreateTransactionUseCase createTransactionUseCase;
    @Mock
    private ListTransactionsUseCase listTransactionsUseCase;
    @Mock
    private GetFinancialSummaryUseCase getFinancialSummaryUseCase;
    @Mock
    private GetCategoryBreakdownUseCase getCategoryBreakdownUseCase;
    @Mock
    private DeleteTransactionUseCase deleteTransactionUseCase;

    private FinancialAssistantTools tools;

    @BeforeEach
    void setUp() {
        tools = new FinancialAssistantTools(
                createTransactionUseCase,
                listTransactionsUseCase,
                getFinancialSummaryUseCase,
                getCategoryBreakdownUseCase,
                deleteTransactionUseCase
        );
    }

    @Test
    void shouldInvokeAllSixToolsAndTrackThemInToolInvocationContext() {
        UUID txId = UUID.randomUUID();
        TransactionOutput sampleOutput = new TransactionOutput(
                txId,
                "Mercado",
                new BigDecimal("80.00"),
                TransactionType.EXPENSE,
                Category.GROCERIES,
                Category.GROCERIES.getDisplayNamePtBr(),
                LocalDate.of(2026, 11, 20),
                TransactionSource.VOICE_AI,
                Instant.now()
        );
        when(createTransactionUseCase.execute(any())).thenReturn(sampleOutput);
        when(listTransactionsUseCase.execute(any())).thenReturn(List.of(sampleOutput));
        when(listTransactionsUseCase.listRecent(5)).thenReturn(List.of(sampleOutput));
        when(getFinancialSummaryUseCase.execute(any(), any())).thenReturn(new FinancialSummary(
                BigDecimal.ZERO, new BigDecimal("80.00"), new BigDecimal("-80.00"),
                0, 1, 1, new BigDecimal("80.00"), null, null
        ));
        when(getCategoryBreakdownUseCase.execute(any(), any())).thenReturn(List.of(
                new CategoryBreakdown(Category.GROCERIES, "Supermercado e Mercearia",
                        new BigDecimal("80.00"), 1, new BigDecimal("100.00"))
        ));
        when(deleteTransactionUseCase.execute(txId.toString())).thenReturn(sampleOutput);

        try (ToolInvocationContext.Scope scope = ToolInvocationContext.open(TransactionSource.VOICE_AI)) {
            tools.registerTransaction("Mercado", new BigDecimal("80.00"),
                    TransactionType.EXPENSE, Category.GROCERIES, "2026-11-20");
            tools.listTransactionsByCategory(Category.GROCERIES);
            tools.listRecentTransactions(null);
            tools.getFinancialSummary("2026-11-01", "2026-11-30");
            tools.getCategorySpendingBreakdown(null, null);
            tools.deleteTransaction(txId.toString());

            assertThat(scope.invokedTools()).containsExactly(
                    "register-transaction",
                    "list-transactions-by-category",
                    "list-recent-transactions",
                    "get-financial-summary",
                    "get-category-spending-breakdown",
                    "delete-transaction"
            );
        }

        ArgumentCaptor<CreateTransactionInput> createCaptor = ArgumentCaptor.forClass(CreateTransactionInput.class);
        verify(createTransactionUseCase).execute(createCaptor.capture());
        assertThat(createCaptor.getValue().source()).isEqualTo(TransactionSource.VOICE_AI);
        assertThat(createCaptor.getValue().transactionDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        verify(listTransactionsUseCase).execute(new TransactionFilter(Category.GROCERIES, null, null, null));
        verify(listTransactionsUseCase).listRecent(5);
    }

    @Test
    void shouldRejectInvalidIsoDateInTools() {
        assertThatThrownBy(() -> tools.getFinancialSummary("20/11/2026", null))
                .isInstanceOf(DomainValidationException.class);
    }
}
