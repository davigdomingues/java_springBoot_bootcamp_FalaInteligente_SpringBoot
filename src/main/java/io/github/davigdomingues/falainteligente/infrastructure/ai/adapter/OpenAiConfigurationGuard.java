package io.github.davigdomingues.falainteligente.infrastructure.ai.adapter;

import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda de configuração que valida a presença de credencial real da OpenAI antes de disparar
 * chamadas remotas de STT, Chat ou TTS.
 *
 * Estrutura e impacto: impede que a aplicação falhe de forma opaca quando executada sem
 * {@code OPENAI_API_KEY}, devolvendo uma orientação objetiva mapeada para HTTP {@code 503}.
 */
@Component
public class OpenAiConfigurationGuard {

    public static final String PLACEHOLDER_KEY = "not-configured";

    private final String apiKey;

    public OpenAiConfigurationGuard(@Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank() || PLACEHOLDER_KEY.equalsIgnoreCase(apiKey.trim())) {
            throw new AiProviderUnavailableException(
                    "OPENAI_API_KEY não configurada. Exporte a variável OPENAI_API_KEY para utilizar os modelos "
                            + "da OpenAI ou inicie a aplicação com o perfil offline '--spring.profiles.active=demo'."
            );
        }
    }
}
