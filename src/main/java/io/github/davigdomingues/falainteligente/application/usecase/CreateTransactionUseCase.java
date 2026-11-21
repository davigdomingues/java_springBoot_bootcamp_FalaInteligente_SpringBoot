package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.input.CreateTransactionInput;
import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso para validação e persistência de uma nova transação financeira.
 *
 * Estrutura e impacto: ponto único de entrada para criação de lançamentos tanto pela API REST
 * quanto pela ferramenta {@code register-transaction} acionada pela IA, garantindo aplicação
 * uniforme dos limites configurados em {@link FalaInteligenteProperties}.
 */
@Service
public class CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final FalaInteligenteProperties properties;
    private final Clock clock;

    public CreateTransactionUseCase(
            TransactionRepository transactionRepository,
            FalaInteligenteProperties properties,
            Clock clock
    ) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public TransactionOutput execute(CreateTransactionInput input) {
        if (input == null) {
            throw new DomainValidationException("Transaction input must not be null");
        }
        if (input.description() != null
                && input.description().trim().length() > properties.transaction().maxDescriptionLength()) {
            throw new DomainValidationException(
                    "Transaction description exceeds maximum configured length of "
                            + properties.transaction().maxDescriptionLength() + " characters"
            );
        }
        BigDecimal maxAmount = properties.transaction().maxAmount();
        if (input.amount() != null && input.amount().compareTo(maxAmount) > 0) {
            throw new DomainValidationException(
                    "Transaction amount exceeds maximum configured limit of " + maxAmount
            );
        }

        Transaction transaction = Transaction.create(
                input.description(),
                input.amount(),
                input.type(),
                input.category(),
                input.transactionDate(),
                input.source(),
                clock
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionOutput.from(saved);
    }
}
