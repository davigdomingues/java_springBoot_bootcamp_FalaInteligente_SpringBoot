package io.github.davigdomingues.falainteligente.domain.exception;

/**
 * Exceção disparada quando a extensão ou mídia do arquivo de áudio não é suportada pelo serviço de transcrição.
 *
 * Estrutura e impacto: mapeada para HTTP {@code 415 Unsupported Media Type}.
 */
public class UnsupportedAudioFormatException extends RuntimeException {

    public UnsupportedAudioFormatException(String message) {
        super(message);
    }
}
