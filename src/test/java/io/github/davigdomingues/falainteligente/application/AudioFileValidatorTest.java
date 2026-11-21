package io.github.davigdomingues.falainteligente.application;

import io.github.davigdomingues.falainteligente.application.input.AudioPayload;
import io.github.davigdomingues.falainteligente.application.service.AudioFileValidator;
import io.github.davigdomingues.falainteligente.domain.exception.InvalidAudioFileException;
import io.github.davigdomingues.falainteligente.domain.exception.UnsupportedAudioFormatException;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties.AudioProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do {@link AudioFileValidator}.
 *
 * Cobertura crítica: aceitação case-insensitive das extensões permitidas e bloqueio preventivo de
 * payloads nulos, vazios, acima do teto configurado ou com extensões não suportadas.
 */
class AudioFileValidatorTest {

    private final AudioFileValidator validator = new AudioFileValidator(FalaInteligenteProperties.defaults());

    @ParameterizedTest
    @ValueSource(strings = {"voice.mp3", "COMMAND.M4A", "note.wav", "clip.ogg", "stream.webm"})
    void shouldAcceptSupportedAudioExtensions(String filename) {
        AudioPayload payload = new AudioPayload(filename, "audio/mpeg", new byte[]{1, 2, 3, 4});
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullOrEmptyAudioFile() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidAudioFileException.class);

        assertThatThrownBy(() -> validator.validate(new AudioPayload("empty.mp3", "audio/mpeg", new byte[0])))
                .isInstanceOf(InvalidAudioFileException.class);

        assertThatThrownBy(() -> validator.validate(new AudioPayload("null.mp3", "audio/mpeg", null)))
                .isInstanceOf(InvalidAudioFileException.class);
    }

    @Test
    void shouldRejectOversizedAudioFile() {
        FalaInteligenteProperties customProps = new FalaInteligenteProperties(
                new AudioProperties(10L, List.of("mp3")),
                null
        );
        AudioFileValidator smallValidator = new AudioFileValidator(customProps);
        AudioPayload oversized = new AudioPayload("large.mp3", "audio/mpeg", new byte[11]);

        assertThatThrownBy(() -> smallValidator.validate(oversized))
                .isInstanceOf(InvalidAudioFileException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"script.exe", "document.pdf", "noextension", "trailingdot.", ""})
    void shouldRejectUnsupportedOrMissingAudioExtensions(String filename) {
        AudioPayload payload = new AudioPayload(filename, "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(UnsupportedAudioFormatException.class);
    }
}
