package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta JSON da rota de transcrição direta ({@code POST /api/v1/assistant/transcribe}).
 */
public record TranscriptionResponse(
        UUID interactionId,
        String text,
        long transcriptionLatencyMs,
        Instant createdAt
) {

    public static TranscriptionResponse from(AssistantCommandOutput output) {
        return new TranscriptionResponse(
                output.interactionId(),
                output.transcribedText(),
                output.transcriptionLatencyMs(),
                output.createdAt()
        );
    }
}
