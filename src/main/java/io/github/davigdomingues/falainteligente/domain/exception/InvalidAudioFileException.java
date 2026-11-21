package io.github.davigdomingues.falainteligente.domain.exception;

/**
 * Exceção disparada quando o arquivo de áudio enviado está ausente, vazio ou excede o limite permitido.
 *
 * Estrutura e impacto: mapeada para HTTP {@code 400 Bad Request} antes de consumir recursos de IA.
 */
public class InvalidAudioFileException extends RuntimeException {

    public InvalidAudioFileException(String message) {
        super(message);
    }
}
