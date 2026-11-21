package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.input.TransactionFilter;
import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para consulta paginada/filtrada e listagem recente de transações financeiras.
 */
@Service
public class ListTransactionsUseCase {

    private static final int MAX_RECENT_LIMIT = 100;

    private final TransactionRepository transactionRepository;

    public ListTransactionsUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<TransactionOutput> execute(TransactionFilter filter) {
        TransactionFilter effectiveFilter = filter != null ? filter : TransactionFilter.empty();
        return transactionRepository.findAll(
                        effectiveFilter.category(),
                        effectiveFilter.type(),
                        effectiveFilter.startDate(),
                        effectiveFilter.endDate()
                )
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionOutput> listRecent(int limit) {
        if (limit <= 0 || limit > MAX_RECENT_LIMIT) {
            throw new DomainValidationException(
                    "Recent transactions limit must be between 1 and " + MAX_RECENT_LIMIT
            );
        }
        return transactionRepository.findRecent(limit)
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
