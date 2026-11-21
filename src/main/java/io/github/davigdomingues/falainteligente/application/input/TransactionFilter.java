package io.github.davigdomingues.falainteligente.application.input;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;

import java.time.LocalDate;

/**
 * Critérios opcionais de filtragem para consulta de transações financeiras.
 *
 * Estrutura e impacto: garante consistência temporal ({@code startDate <= endDate}) antes de
 * repassar a consulta ao repositório.
 */
public record TransactionFilter(
        Category category,
        TransactionType type,
        LocalDate startDate,
        LocalDate endDate
) {

    public TransactionFilter {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainValidationException("startDate must not be after endDate");
        }
    }

    public static TransactionFilter empty() {
        return new TransactionFilter(null, null, null, null);
    }
}
