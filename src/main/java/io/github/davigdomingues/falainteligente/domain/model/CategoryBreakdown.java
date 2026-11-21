package io.github.davigdomingues.falainteligente.domain.model;

import io.github.davigdomingues.falainteligente.domain.enums.Category;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Detalhamento de despesas agrupadas por categoria com participação percentual no total gasto.
 *
 * Estrutura e impacto: permite identificar rapidamente os maiores ofensores do orçamento em
 * relatórios REST e respostas inteligentes da IA.
 */
public record CategoryBreakdown(
        Category category,
        String categoryDisplayName,
        BigDecimal totalAmount,
        long transactionCount,
        BigDecimal percentageOfTotal
) {

    public CategoryBreakdown {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(categoryDisplayName, "categoryDisplayName must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(percentageOfTotal, "percentageOfTotal must not be null");
    }
}
