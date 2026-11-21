package io.github.davigdomingues.falainteligente.infrastructure.http.controller;

import io.github.davigdomingues.falainteligente.application.usecase.GetAiMetricsUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ListAiAuditLogsUseCase;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.AiInteractionAuditResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.AiMetricsSummaryResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador REST para inspeção da trilha de auditoria e métricas operacionais da camada de IA.
 *
 * Estrutura e impacto: atende ao requisito de evolução e auditoria da API expondo o histórico
 * completo das execuções ({@code SUCCESS} e {@code FAILED}), latências médias e ferramentas mais usadas.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoria e Métricas de IA", description = "Trilha de auditoria e telemetria das interações com IA")
public class AuditController {

    private final ListAiAuditLogsUseCase listAiAuditLogsUseCase;
    private final GetAiMetricsUseCase getAiMetricsUseCase;

    public AuditController(
            ListAiAuditLogsUseCase listAiAuditLogsUseCase,
            GetAiMetricsUseCase getAiMetricsUseCase
    ) {
        this.listAiAuditLogsUseCase = Objects.requireNonNull(
                listAiAuditLogsUseCase, "listAiAuditLogsUseCase must not be null");
        this.getAiMetricsUseCase = Objects.requireNonNull(
                getAiMetricsUseCase, "getAiMetricsUseCase must not be null");
    }

    @GetMapping("/interactions")
    @Operation(summary = "Listar registros de auditoria de interações com IA com filtros opcionais de status e canal")
    public ResponseEntity<List<AiInteractionAuditResponse>> listInteractions(
            @RequestParam(required = false) InteractionStatus status,
            @RequestParam(required = false) InteractionChannel channel
    ) {
        List<AiInteractionAuditResponse> response = listAiAuditLogsUseCase
                .execute(status, channel)
                .stream()
                .map(AiInteractionAuditResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/interactions/{id}")
    @Operation(summary = "Consultar registro de auditoria de interação com IA por UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro de auditoria encontrado"),
            @ApiResponse(responseCode = "404", description = "Registro de auditoria não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AiInteractionAuditResponse> getInteraction(@PathVariable UUID id) {
        return ResponseEntity.ok(AiInteractionAuditResponse.from(listAiAuditLogsUseCase.getById(id)));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Obter métricas agregadas de IA (taxa de sucesso, latências e contagem de uso de ferramentas)")
    public ResponseEntity<AiMetricsSummaryResponse> getMetrics() {
        return ResponseEntity.ok(AiMetricsSummaryResponse.from(getAiMetricsUseCase.execute()));
    }
}
