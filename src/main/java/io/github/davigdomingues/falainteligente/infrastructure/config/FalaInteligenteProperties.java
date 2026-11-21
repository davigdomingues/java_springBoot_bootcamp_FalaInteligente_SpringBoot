package io.github.davigdomingues.falainteligente.infrastructure.config;

import io.github.davigdomingues.falainteligente.domain.model.Transaction;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Propriedades imutáveis de configuração do domínio e limites operacionais da API Fala Inteligente.
 *
 * Estrutura e impacto: centraliza via {@link ConfigurationProperties} os limites de upload de
 * áudio (tamanho e extensões aceitas) e teto financeiro das transações, com valores padrão seguros
 * para inicialização sem configuração explícita.
 */
@ConfigurationProperties(prefix = "fala-inteligente")
public record FalaInteligenteProperties(
        AudioProperties audio,
        TransactionProperties transaction
) {

    public static final long DEFAULT_MAX_AUDIO_SIZE_BYTES = 15_728_640L;
    public static final List<String> DEFAULT_ALLOWED_EXTENSIONS =
            List.of("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm", "ogg");

    public FalaInteligenteProperties {
        audio = audio != null ? audio : new AudioProperties(DEFAULT_MAX_AUDIO_SIZE_BYTES, DEFAULT_ALLOWED_EXTENSIONS);
        transaction = transaction != null
                ? transaction
                : new TransactionProperties(Transaction.DEFAULT_MAX_AMOUNT, Transaction.MAX_DESCRIPTION_LENGTH);
    }

    public static FalaInteligenteProperties defaults() {
        return new FalaInteligenteProperties(null, null);
    }

    /**
     * Regras de validação aplicáveis ao recebimento de arquivos de áudio nos endpoints de voz.
     */
    public record AudioProperties(
            long maxSizeBytes,
            List<String> allowedExtensions
    ) {
        public AudioProperties {
            if (maxSizeBytes <= 0) {
                maxSizeBytes = DEFAULT_MAX_AUDIO_SIZE_BYTES;
            }
            if (allowedExtensions == null || allowedExtensions.isEmpty()) {
                allowedExtensions = DEFAULT_ALLOWED_EXTENSIONS;
            } else {
                allowedExtensions = allowedExtensions.stream()
                        .filter(ext -> ext != null && !ext.isBlank())
                        .map(ext -> ext.trim().toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
            }
        }
    }

    /**
     * Limites operacionais para criação e validação de transações financeiras.
     */
    public record TransactionProperties(
            BigDecimal maxAmount,
            int maxDescriptionLength
    ) {
        public TransactionProperties {
            if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
                maxAmount = Transaction.DEFAULT_MAX_AMOUNT;
            }
            if (maxDescriptionLength <= 0 || maxDescriptionLength > Transaction.MAX_DESCRIPTION_LENGTH) {
                maxDescriptionLength = Transaction.MAX_DESCRIPTION_LENGTH;
            }
        }
    }
}
