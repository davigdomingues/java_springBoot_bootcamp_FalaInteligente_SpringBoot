package io.github.davigdomingues.falainteligente.domain.exception;

/**
 * Exceção lançada quando um recurso financeiro ou registro de auditoria não é localizado.
 *
 * Estrutura e impacto: mapeada centralizadamente para HTTP {@code 404 Not Found}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
