package io.github.davigdomingues.falainteligente.domain.model;

import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object fortemente tipado para o identificador único de uma transação financeira.
 *
 * Estrutura e impacto: evita troca acidental de argumentos {@code UUID} ou {@code String} entre
 * casos de uso e adaptadores, validando o formato canônico logo na borda do domínio.
 */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId value must not be null");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    /**
     * Converte uma representação textual em {@link TransactionId}, lançando exceção de domínio
     * caso o identificador seja nulo, vazio ou inválido.
     */
    public static TransactionId fromString(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new DomainValidationException("Transaction id must not be blank");
        }
        try {
            return new TransactionId(UUID.fromString(rawId.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid transaction id format: " + rawId);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
