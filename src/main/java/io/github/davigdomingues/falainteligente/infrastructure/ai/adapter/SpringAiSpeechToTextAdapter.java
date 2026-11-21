package io.github.davigdomingues.falainteligente.infrastructure.ai.adapter;

import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.port.SpeechToTextPort;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adaptador de saída Spring AI 2.0 para transcrição de áudio (Speech-to-Text) via {@link TranscriptionModel}.
 *
 * Estrutura e impacto: sobrescreve {@link ByteArrayResource#getFilename()} preservando o nome e
 * a extensão originais do arquivo, requisito obrigatório para que a API Whisper reconheça o codec
 * multipart sem rejeição.
 */
@Component
@Profile("!demo")
public class SpringAiSpeechToTextAdapter implements SpeechToTextPort {

    private final TranscriptionModel transcriptionModel;
    private final OpenAiConfigurationGuard configurationGuard;

    public SpringAiSpeechToTextAdapter(
            TranscriptionModel transcriptionModel,
            OpenAiConfigurationGuard configurationGuard
    ) {
        this.transcriptionModel = Objects.requireNonNull(
                transcriptionModel, "transcriptionModel must not be null");
        this.configurationGuard = Objects.requireNonNull(
                configurationGuard, "configurationGuard must not be null");
    }

    @Override
    public String transcribe(String filename, String contentType, byte[] audioBytes) {
        configurationGuard.ensureConfigured();
        String effectiveFilename = (filename == null || filename.isBlank()) ? "audio.mp3" : filename.trim();

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return effectiveFilename;
            }
        };

        try {
            return transcriptionModel.transcribe(audioResource);
        } catch (AiProviderUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiProviderUnavailableException(
                    "Falha ao transcrever áudio no provedor OpenAI: " + ex.getMessage(),
                    ex
            );
        }
    }
}
