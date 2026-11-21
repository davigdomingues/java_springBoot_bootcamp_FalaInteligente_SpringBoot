package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Caso de uso para recuperação individual de uma transação pelo seu identificador UUID.
 */
@Service
public class GetTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Transactional(readOnly = true)
    public TransactionOutput execute(String rawTransactionId) {
        return execute(TransactionId.fromString(rawTransactionId));
    }

    @Transactional(readOnly = true)
    public TransactionOutput execute(TransactionId transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        return transactionRepository.findById(transactionId)
                .map(TransactionOutput::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }
}
