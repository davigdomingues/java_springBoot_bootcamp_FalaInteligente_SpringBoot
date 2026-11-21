package io.github.davigdomingues.falainteligente.domain;

import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import io.github.davigdomingues.falainteligente.domain.model.TransactionId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários puros das invariantes do domínio ({@link Transaction}, {@link TransactionId} e
 * {@link AiInteractionAudit}).
 *
 * Cobertura crítica: normalização monetária com {@code HALF_EVEN}, higienização de descrição,
 * validação de limites máximos/mínimos e rejeição de identificadores inválidos.
 */
class TransactionDomainTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateTransactionWithTrimmedDescriptionHalfEvenRoundingAndDefaultDate() {
        Transaction tx = Transaction.create(
                "  Compras no mercado  ",
                new BigDecimal("10.125"),
                TransactionType.EXPENSE,
                Category.GROCERIES,
                null,
                TransactionSource.VOICE_AI,
                FIXED_CLOCK
        );

        assertThat(tx.id()).isNotNull();
        assertThat(tx.description()).isEqualTo("Compras no mercado");
        assertThat(tx.amount()).isEqualByComparingTo("10.12");
        assertThat(tx.transactionDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(tx.createdAt()).isEqualTo(Instant.parse("2026-11-20T15:00:00Z"));
        assertThat(tx.source()).isEqualTo(TransactionSource.VOICE_AI);
    }

    @Test
    void shouldRejectInvalidTransactionInvariants() {
        assertThatThrownBy(() -> Transaction.create(
                " ",
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                Category.OTHER,
                LocalDate.now(FIXED_CLOCK),
                TransactionSource.REST,
                FIXED_CLOCK
        )).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> Transaction.create(
                "A",
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                Category.OTHER,
                LocalDate.now(FIXED_CLOCK),
                TransactionSource.REST,
                FIXED_CLOCK
        )).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> Transaction.create(
                "Compra válida",
                BigDecimal.ZERO,
                TransactionType.EXPENSE,
                Category.OTHER,
                LocalDate.now(FIXED_CLOCK),
                TransactionSource.REST,
                FIXED_CLOCK
        )).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> Transaction.create(
                "Compra milionária",
                new BigDecimal("1000000.01"),
                TransactionType.EXPENSE,
                Category.OTHER,
                LocalDate.now(FIXED_CLOCK),
                TransactionSource.REST,
                FIXED_CLOCK
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldParseValidTransactionIdAndRejectMalformedValues() {
        UUID raw = UUID.randomUUID();
        TransactionId parsed = TransactionId.fromString(" " + raw + " ");

        assertThat(parsed.value()).isEqualTo(raw);
        assertThat(parsed.toString()).isEqualTo(raw.toString());

        assertThatThrownBy(() -> TransactionId.fromString("not-a-uuid"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> TransactionId.fromString(" "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldRejectNegativeLatencyInAiInteractionAudit() {
        assertThatThrownBy(() -> new AiInteractionAudit(
                UUID.randomUUID(),
                InteractionChannel.VOICE,
                "cmd.m4a",
                100L,
                "oi",
                "ola",
                List.of(),
                InteractionStatus.SUCCESS,
                null,
                -1L,
                10L,
                10L,
                20L,
                Instant.now(FIXED_CLOCK)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
