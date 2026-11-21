package io.github.davigdomingues.falainteligente.application.service;

import io.github.davigdomingues.falainteligente.application.input.AudioPayload;
import io.github.davigdomingues.falainteligente.domain.exception.InvalidAudioFileException;
import io.github.davigdomingues.falainteligente.domain.exception.UnsupportedAudioFormatException;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * Validador defensivo de arquivos de áudio enviados aos endpoints de voz e transcrição.
 *
 * Estrutura e impacto: bloqueia payloads vazios, arquivos acima do tamanho máximo configurado e
 * extensões incompatíveis antes de acionar a API de Speech-to-Text, economizando custo e latência.
 */
@Service
public class AudioFileValidator {

    private final FalaInteligenteProperties properties;

    public AudioFileValidator(FalaInteligenteProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Valida integridade, tamanho máximo e extensão suportada do áudio informado.
     *
     * @throws InvalidAudioFileException quando o arquivo está ausente, vazio ou excede o limite
     * @throws UnsupportedAudioFormatException quando a extensão não faz parte do catálogo permitido
     */
    public void validate(AudioPayload audio) {
        if (audio == null || audio.bytes() == null || audio.bytes().length == 0) {
            throw new InvalidAudioFileException("Audio file must not be empty");
        }

        long maxSizeBytes = properties.audio().maxSizeBytes();
        if (audio.bytes().length > maxSizeBytes) {
            throw new InvalidAudioFileException(
                    "Audio file size (" + audio.bytes().length + " bytes) exceeds maximum allowed of "
                            + maxSizeBytes + " bytes"
            );
        }

        String extension = extractExtension(audio.filename());
        if (extension == null) {
            throw new UnsupportedAudioFormatException(
                    "Audio filename must include a valid extension. Supported formats: "
                            + properties.audio().allowedExtensions()
            );
        }

        if (!properties.audio().allowedExtensions().contains(extension)) {
            throw new UnsupportedAudioFormatException(
                    "Unsupported audio format '." + extension + "'. Supported formats: "
                            + properties.audio().allowedExtensions()
            );
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String trimmed = filename.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == trimmed.length() - 1) {
            return null;
        }
        return trimmed.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
