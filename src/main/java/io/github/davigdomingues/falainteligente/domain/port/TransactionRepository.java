package io.github.davigdomingues.falainteligente.domain.port;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para persistência e consulta de transações financeiras.
 *
 * Estrutura e impacto: desacopla totalmente o domínio e os casos de uso do Spring Data JPA,
 * permitindo evolução de banco (H2, PostgreSQL) ou mocks em testes unitários sem impacto no core.
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(TransactionId id);

    List<Transaction> findAll(Category category, TransactionType type, LocalDate startDate, LocalDate endDate);

    List<Transaction> findRecent(int limit);

    void deleteById(TransactionId id);

    boolean existsById(TransactionId id);
}
