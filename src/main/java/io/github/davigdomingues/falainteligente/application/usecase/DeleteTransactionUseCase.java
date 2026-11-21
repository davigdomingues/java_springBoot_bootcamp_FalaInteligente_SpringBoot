package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Caso de uso para exclusão segura de uma transação financeira existente.
 *
 * Estrutura e impacto: utilizado pela rota {@code DELETE /api/v1/transactions/{id}} e pela
 * ferramenta {@code delete-transaction} do assistente, retornando os dados do registro removido
 * para confirmação clara ao usuário.
 */
@Service
public class DeleteTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public DeleteTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Transactional
    public TransactionOutput execute(String rawTransactionId) {
        return execute(TransactionId.fromString(rawTransactionId));
    }

    @Transactional
    public TransactionOutput execute(TransactionId transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        transactionRepository.deleteById(transactionId);
        return TransactionOutput.from(existing);
    }
}
