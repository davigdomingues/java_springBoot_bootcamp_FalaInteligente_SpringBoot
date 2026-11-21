package io.github.davigdomingues.falainteligente.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload REST para envio de instruções financeiras em linguagem natural por texto.
 */
public record TextCommandRequest(
        @Schema(description = "Comando em português brasileiro para o assistente financeiro",
                example = "Gastei 120 reais com gasolina hoje")
        @NotBlank(message = "message must not be blank")
        @Size(min = 2, max = 1000, message = "message must have between 2 and 1000 characters")
        String message,

        @Schema(description = "Indica se a resposta deve incluir áudio MP3 codificado em Base64",
                example = "false")
        boolean includeAudio
) {
}
