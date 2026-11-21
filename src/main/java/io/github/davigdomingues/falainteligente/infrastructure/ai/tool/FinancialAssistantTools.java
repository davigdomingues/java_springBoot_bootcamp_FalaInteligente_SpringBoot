package io.github.davigdomingues.falainteligente.infrastructure.ai.tool;

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
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Catálogo declarativo de ferramentas ({@code @Tool}) expostas ao {@code ChatClient} do Spring AI 2.0.
 *
 * Estrutura e impacto: atua como adaptador de entrada para o LLM, delegando todas as operações aos
 * casos de uso reais da aplicação sem jamais acessar repositórios JPA diretamente e registrando
 * cada invocação no {@link ToolInvocationContext} para rastreabilidade em auditoria.
 */
@Component
public class FinancialAssistantTools {

    private static final int DEFAULT_RECENT_LIMIT = 5;
    private static final int MAX_TOOL_RECENT_LIMIT = 50;

    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final GetFinancialSummaryUseCase getFinancialSummaryUseCase;
    private final GetCategoryBreakdownUseCase getCategoryBreakdownUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;

    public FinancialAssistantTools(
            CreateTransactionUseCase createTransactionUseCase,
            ListTransactionsUseCase listTransactionsUseCase,
            GetFinancialSummaryUseCase getFinancialSummaryUseCase,
            GetCategoryBreakdownUseCase getCategoryBreakdownUseCase,
            DeleteTransactionUseCase deleteTransactionUseCase
    ) {
        this.createTransactionUseCase = Objects.requireNonNull(
                createTransactionUseCase, "createTransactionUseCase must not be null");
        this.listTransactionsUseCase = Objects.requireNonNull(
                listTransactionsUseCase, "listTransactionsUseCase must not be null");
        this.getFinancialSummaryUseCase = Objects.requireNonNull(
                getFinancialSummaryUseCase, "getFinancialSummaryUseCase must not be null");
        this.getCategoryBreakdownUseCase = Objects.requireNonNull(
                getCategoryBreakdownUseCase, "getCategoryBreakdownUseCase must not be null");
        this.deleteTransactionUseCase = Objects.requireNonNull(
                deleteTransactionUseCase, "deleteTransactionUseCase must not be null");
    }

    @Tool(
            name = "register-transaction",
            description = "Registra uma nova transação financeira (despesa EXPENSE ou receita INCOME) no orçamento do usuário."
    )
    public TransactionOutput registerTransaction(
            @ToolParam(description = "Descrição clara e breve da transação, por exemplo 'Compras no supermercado'")
            String description,
            @ToolParam(description = "Valor monetário positivo em reais, por exemplo 85.50")
            BigDecimal amount,
            @ToolParam(description = "Tipo da transação: EXPENSE para despesa ou INCOME para receita")
            TransactionType type,
            @ToolParam(description = "Categoria da transação: GROCERIES, PHARMA, AUTO, FOOD_DELIVERY, HOUSING, UTILITIES, LEISURE, HEALTH, EDUCATION, SALARY, FREELANCE, INVESTMENTS ou OTHER")
            Category category,
            @ToolParam(required = false, description = "Data opcional no formato YYYY-MM-DD; se omitida, assume a data atual")
            String transactionDate
    ) {
        ToolInvocationContext.recordTool("register-transaction");
        TransactionSource source = ToolInvocationContext.currentSourceOrDefault(TransactionSource.TEXT_AI);
        LocalDate parsedDate = parseOptionalDate(transactionDate);
        return createTransactionUseCase.execute(new CreateTransactionInput(
                description,
                amount,
                type,
                category,
                parsedDate,
                source
        ));
    }

    @Tool(
            name = "list-transactions-by-category",
            description = "Lista todas as transações financeiras cadastradas em uma categoria específica."
    )
    public List<TransactionOutput> listTransactionsByCategory(
            @ToolParam(description = "Categoria financeira para filtragem")
            Category category
    ) {
        ToolInvocationContext.recordTool("list-transactions-by-category");
        return listTransactionsUseCase.execute(new TransactionFilter(category, null, null, null));
    }

    @Tool(
            name = "list-recent-transactions",
            description = "Lista as transações financeiras mais recentes do usuário em ordem decrescente de data."
    )
    public List<TransactionOutput> listRecentTransactions(
            @ToolParam(required = false, description = "Quantidade máxima de transações recentes a listar (padrão 5, máximo 50)")
            Integer limit
    ) {
        ToolInvocationContext.recordTool("list-recent-transactions");
        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_RECENT_LIMIT
                : Math.min(limit, MAX_TOOL_RECENT_LIMIT);
        return listTransactionsUseCase.listRecent(effectiveLimit);
    }

    @Tool(
            name = "get-financial-summary",
            description = "Retorna o resumo financeiro consolidado com receitas, despesas, saldo líquido e ticket médio, podendo filtrar por período."
    )
    public FinancialSummary getFinancialSummary(
            @ToolParam(required = false, description = "Data inicial opcional no formato YYYY-MM-DD")
            String startDate,
            @ToolParam(required = false, description = "Data final opcional no formato YYYY-MM-DD")
            String endDate
    ) {
        ToolInvocationContext.recordTool("get-financial-summary");
        return getFinancialSummaryUseCase.execute(parseOptionalDate(startDate), parseOptionalDate(endDate));
    }

    @Tool(
            name = "get-category-spending-breakdown",
            description = "Retorna o detalhamento de despesas agrupadas por categoria, com valor total, quantidade e percentual de participação."
    )
    public List<CategoryBreakdown> getCategorySpendingBreakdown(
            @ToolParam(required = false, description = "Data inicial opcional no formato YYYY-MM-DD")
            String startDate,
            @ToolParam(required = false, description = "Data final opcional no formato YYYY-MM-DD")
            String endDate
    ) {
        ToolInvocationContext.recordTool("get-category-spending-breakdown");
        return getCategoryBreakdownUseCase.execute(parseOptionalDate(startDate), parseOptionalDate(endDate));
    }

    @Tool(
            name = "delete-transaction",
            description = "Remove permanentemente uma transação financeira existente pelo seu identificador UUID."
    )
    public TransactionOutput deleteTransaction(
            @ToolParam(description = "Identificador UUID da transação a ser excluída")
            String transactionId
    ) {
        ToolInvocationContext.recordTool("delete-transaction");
        return deleteTransactionUseCase.execute(transactionId);
    }

    private LocalDate parseOptionalDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ex) {
            throw new DomainValidationException("Invalid date format '" + rawDate + "', expected YYYY-MM-DD");
        }
    }
}
