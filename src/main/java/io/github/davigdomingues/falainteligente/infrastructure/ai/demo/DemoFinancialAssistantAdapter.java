package io.github.davigdomingues.falainteligente.infrastructure.ai.demo;

import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.application.service.ToolInvocationContext;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;
import io.github.davigdomingues.falainteligente.domain.model.FinancialSummary;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.infrastructure.ai.tool.FinancialAssistantTools;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Assistente financeiro determinístico offline ativado no perfil {@code demo}.
 *
 * Estrutura e impacto: interpreta intenções comuns em português brasileiro e aciona exatamente os
 * mesmos métodos {@code @Tool} de {@link FinancialAssistantTools} dentro do
 * {@link ToolInvocationContext}, permitindo validar persistência real, auditoria e métricas sem
 * exigir chave da OpenAI.
 */
@Component
@Profile("demo")
public class DemoFinancialAssistantAdapter implements FinancialAssistantPort {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)");
    private static final Pattern THOUSANDS_ONLY_PATTERN =
            Pattern.compile("^\\d{1,3}(?:\\.\\d{3})+$");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final FinancialAssistantTools tools;
    private final Clock clock;

    public DemoFinancialAssistantAdapter(FinancialAssistantTools tools, Clock clock) {
        this.tools = Objects.requireNonNull(tools, "tools must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AssistantReply process(String userMessage, TransactionSource source) {
        String message = userMessage == null ? "" : userMessage.trim();
        String lower = message.toLowerCase(PT_BR);

        try (ToolInvocationContext.Scope scope = ToolInvocationContext.open(source)) {
            String responseText = routeIntent(message, lower);
            return new AssistantReply(responseText, scope.invokedTools());
        }
    }

    private String routeIntent(String originalMessage, String lower) {
        Matcher uuidMatcher = UUID_PATTERN.matcher(originalMessage);
        if (uuidMatcher.find() && containsAny(lower, "excluir", "deletar", "remover", "apagar")) {
            TransactionOutput deleted = tools.deleteTransaction(uuidMatcher.group());
            return "Transação " + deleted.description() + " no valor de "
                    + formatCurrency(deleted.amount()) + " foi removida com sucesso.";
        }

        if (containsAny(lower, "resumo", "saldo", "balanço", "balanco")) {
            FinancialSummary summary = tools.getFinancialSummary(null, null);
            return "Seu resumo financeiro atual tem "
                    + formatCurrency(summary.totalIncome()) + " de receitas, "
                    + formatCurrency(summary.totalExpenses()) + " de despesas e saldo líquido de "
                    + formatCurrency(summary.balance()) + " em " + summary.totalCount() + " transações.";
        }

        if (containsAny(lower, "por categoria", "categorias", "ranking", "onde gastei")) {
            List<CategoryBreakdown> breakdown = tools.getCategorySpendingBreakdown(null, null);
            if (breakdown.isEmpty()) {
                return "Você ainda não possui despesas registradas para agrupar por categoria.";
            }
            String details = breakdown.stream()
                    .map(item -> item.categoryDisplayName() + " com "
                            + formatCurrency(item.totalAmount()) + " (" + item.percentageOfTotal() + " por cento)")
                    .collect(Collectors.joining("; "));
            return "Distribuição das despesas por categoria: " + details + ".";
        }

        if (containsAny(lower, "listar", "liste", "últimas", "ultimas", "recentes", "extrato", "quais")) {
            Category inferredCategory = inferCategory(lower, false);
            if (inferredCategory != null) {
                List<TransactionOutput> items = tools.listTransactionsByCategory(inferredCategory);
                if (items.isEmpty()) {
                    return "Não há transações registradas na categoria " + inferredCategory.getDisplayNamePtBr() + ".";
                }
                return "Encontrei " + items.size() + " transações em " + inferredCategory.getDisplayNamePtBr()
                        + ", sendo a mais recente " + items.getFirst().description() + " de "
                        + formatCurrency(items.getFirst().amount()) + ".";
            }
            List<TransactionOutput> recent = tools.listRecentTransactions(5);
            if (recent.isEmpty()) {
                return "Nenhuma transação financeira foi registrada até o momento.";
            }
            return "Você tem " + recent.size() + " transações recentes listadas, começando por "
                    + recent.getFirst().description() + " no valor de " + formatCurrency(recent.getFirst().amount()) + ".";
        }

        BigDecimal amount = extractAmount(originalMessage);
        if (amount != null) {
            TransactionType type = containsAny(lower, "recebi", "ganhei", "salário", "salario", "freelance", "rendimento", "dividendo", "entrada")
                    ? TransactionType.INCOME
                    : TransactionType.EXPENSE;
            Category category = inferCategory(lower, true);
            String description = sanitizeDescription(originalMessage, category);
            LocalDate date = lower.contains("ontem")
                    ? LocalDate.now(clock).minusDays(1)
                    : LocalDate.now(clock);

            TransactionOutput created = tools.registerTransaction(
                    description,
                    amount,
                    type,
                    category,
                    date.toString()
            );

            String verb = type == TransactionType.INCOME ? "Receita" : "Despesa";
            return verb + " registrada com sucesso: " + created.description() + " no valor de "
                    + formatCurrency(created.amount()) + " na categoria " + created.categoryDisplayName() + ".";
        }

        return "Modo demo ativo. Posso registrar despesas ou receitas com valores em reais, consultar seu resumo "
                + "financeiro, detalhar gastos por categoria, listar transações recentes ou excluir por UUID.";
    }

    private BigDecimal extractAmount(String message) {
        Matcher matcher = AMOUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        if (raw.contains(",") && raw.contains(".")) {
            raw = raw.replace(".", "").replace(",", ".");
        } else if (raw.contains(",")) {
            raw = raw.replace(",", ".");
        } else if (THOUSANDS_ONLY_PATTERN.matcher(raw).matches()) {
            raw = raw.replace(".", "");
        }
        return new BigDecimal(raw);
    }

    private Category inferCategory(String lower, boolean fallbackToOther) {
        if (containsAny(lower, "mercado", "supermercado", "padaria", "feira", "hortifruti")) {
            return Category.GROCERIES;
        }
        if (containsAny(lower, "farmácia", "farmacia", "remédio", "remedio", "drogaria")) {
            return Category.PHARMA;
        }
        if (containsAny(lower, "gasolina", "combustível", "combustivel", "uber", "carro", "estacionamento", "pedágio")) {
            return Category.AUTO;
        }
        if (containsAny(lower, "ifood", "restaurante", "lanche", "almoço", "almoco", "jantar", "café", "delivery")) {
            return Category.FOOD_DELIVERY;
        }
        if (containsAny(lower, "aluguel", "condomínio", "condominio", "iptu", "moradia")) {
            return Category.HOUSING;
        }
        if (containsAny(lower, "luz", "energia", "água", "agua", "internet", "telefone", "gás")) {
            return Category.UTILITIES;
        }
        if (containsAny(lower, "cinema", "lazer", "viagem", "streaming", "show", "ingresso")) {
            return Category.LEISURE;
        }
        if (containsAny(lower, "médico", "medico", "consulta", "exame", "academia", "saúde", "saude")) {
            return Category.HEALTH;
        }
        if (containsAny(lower, "curso", "livro", "faculdade", "escola", "educação", "educacao")) {
            return Category.EDUCATION;
        }
        if (containsAny(lower, "salário", "salario", "holerite")) {
            return Category.SALARY;
        }
        if (containsAny(lower, "freelance", "freela", "consultoria", "projeto")) {
            return Category.FREELANCE;
        }
        if (containsAny(lower, "investimento", "dividendo", "rendimento", "cdb", "tesouro")) {
            return Category.INVESTMENTS;
        }
        return fallbackToOther ? Category.OTHER : null;
    }

    private String sanitizeDescription(String message, Category category) {
        String cleaned = message.trim();
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120);
        }
        return cleaned.length() >= 2 ? cleaned : category.getDisplayNamePtBr();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
