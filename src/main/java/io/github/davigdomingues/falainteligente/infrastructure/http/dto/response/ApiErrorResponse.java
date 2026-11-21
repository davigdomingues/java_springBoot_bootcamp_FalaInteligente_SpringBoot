package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Contrato padronizado de resposta de erro da API REST.
 *
 * Estrutura e impacto: garante consistência semântica entre todos os códigos HTTP ({@code 400},
 * {@code 404}, {@code 413}, {@code 415}, {@code 422}, {@code 500} e {@code 503}), expondo timestamp,
 * rota solicitada e detalhes específicos sem vazar stack traces.
 */
public record ApiErrorResponse(
        @Schema(description = "Instante UTC da ocorrência do erro")
        Instant timestamp,
        @Schema(description = "Código de status HTTP", example = "422")
        int status,
        @Schema(description = "Categoria resumida do erro", example = "Domain Validation Error")
        String error,
        @Schema(description = "Mensagem descritiva segura")
        String message,
        @Schema(description = "URI da requisição que originou a falha", example = "/api/v1/transactions")
        String path,
        @Schema(description = "Lista de violações ou campos inválidos")
        List<String> details
) {

    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
