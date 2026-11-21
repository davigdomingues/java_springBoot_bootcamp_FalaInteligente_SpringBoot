package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.model.CategoryBreakdown;

import java.math.BigDecimal;

/**
 * Resposta JSON do detalhamento de despesas agrupadas por categoria.
 */
public record CategoryBreakdownResponse(
        Category category,
        String categoryDisplayName,
        BigDecimal totalAmount,
        long transactionCount,
        BigDecimal percentageOfTotal
) {

    public static CategoryBreakdownResponse from(CategoryBreakdown breakdown) {
        return new CategoryBreakdownResponse(
                breakdown.category(),
                breakdown.categoryDisplayName(),
                breakdown.totalAmount(),
                breakdown.transactionCount(),
                breakdown.percentageOfTotal()
        );
    }
}
