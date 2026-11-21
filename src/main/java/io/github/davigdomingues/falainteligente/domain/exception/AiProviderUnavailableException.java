package io.github.davigdomingues.falainteligente.domain.exception;

/**
 * Exceção disparada quando o provedor externo de IA está sem credenciais ou temporariamente indisponível.
 *
 * Estrutura e impacto: isola falhas de integração (OpenAI STT/Chat/TTS) em HTTP {@code 503 Service Unavailable}
 * com orientação clara ao consumidor sobre configuração de chave ou uso do perfil {@code demo}.
 */
public class AiProviderUnavailableException extends RuntimeException {

    public AiProviderUnavailableException(String message) {
        super(message);
    }

    public AiProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
