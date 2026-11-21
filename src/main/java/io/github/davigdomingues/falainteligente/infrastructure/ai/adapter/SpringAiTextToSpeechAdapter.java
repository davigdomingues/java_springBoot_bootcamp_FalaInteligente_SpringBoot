package io.github.davigdomingues.falainteligente.infrastructure.ai.adapter;

import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adaptador de saída Spring AI 2.0 para síntese de voz (Text-to-Speech) via {@link TextToSpeechModel}.
 */
@Component
@Profile("!demo")
public class SpringAiTextToSpeechAdapter implements TextToSpeechPort {

    private final TextToSpeechModel textToSpeechModel;
    private final OpenAiConfigurationGuard configurationGuard;

    public SpringAiTextToSpeechAdapter(
            TextToSpeechModel textToSpeechModel,
            OpenAiConfigurationGuard configurationGuard
    ) {
        this.textToSpeechModel = Objects.requireNonNull(textToSpeechModel, "textToSpeechModel must not be null");
        this.configurationGuard = Objects.requireNonNull(
                configurationGuard, "configurationGuard must not be null");
    }

    @Override
    public byte[] synthesize(String text) {
        configurationGuard.ensureConfigured();
        try {
            byte[] audio = textToSpeechModel.call(text);
            if (audio == null || audio.length == 0) {
                throw new AiProviderUnavailableException("OpenAI Text-to-Speech retornou áudio vazio");
            }
            return audio;
        } catch (AiProviderUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiProviderUnavailableException(
                    "Falha ao sintetizar áudio no provedor OpenAI: " + ex.getMessage(),
                    ex
            );
        }
    }
}
