package io.github.davigdomingues.falainteligente.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload REST para síntese direta de fala (Text-to-Speech).
 */
public record SpeechSynthesisRequest(
        @Schema(description = "Texto a ser convertido em áudio MP3",
                example = "Seu saldo atual é de quatro mil reais.")
        @NotBlank(message = "text must not be blank")
        @Size(min = 1, max = 4000, message = "text must have between 1 and 4000 characters")
        String text
) {
}
