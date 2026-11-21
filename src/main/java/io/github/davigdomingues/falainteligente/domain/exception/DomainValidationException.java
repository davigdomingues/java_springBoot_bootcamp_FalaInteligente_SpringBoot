package io.github.davigdomingues.falainteligente.domain.exception;

import java.util.List;

/**
 * Exceção de domínio disparada quando uma invariante de negócio ou regra semântica é violada.
 *
 * Estrutura e impacto: traduzida pelo {@code GlobalExceptionHandler} em HTTP {@code 422 Unprocessable Content},
 * preservando eventuais detalhes de validação sem acoplar o domínio ao pacote HTTP.
 */
public class DomainValidationException extends RuntimeException {

    private final List<String> details;

    public DomainValidationException(String message) {
        this(message, List.of());
    }

    public DomainValidationException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
