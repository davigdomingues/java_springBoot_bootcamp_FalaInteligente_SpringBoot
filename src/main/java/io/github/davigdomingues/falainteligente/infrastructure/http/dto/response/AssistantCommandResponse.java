package io.github.davigdomingues.falainteligente.infrastructure.http.dto.response;

import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Resposta JSON detalhada das rotas {@code /api/v1/assistant/voice/details} e {@code /api/v1/assistant/text}.
 */
public record AssistantCommandResponse(
        UUID interactionId,
        String transcribedText,
        String assistantResponse,
        List<String> toolsInvoked,
        String audioBase64,
        long transcriptionLatencyMs,
        long reasoningLatencyMs,
        long speechLatencyMs,
        long totalLatencyMs,
        Instant createdAt
) {

    public static AssistantCommandResponse from(AssistantCommandOutput output, boolean includeAudioBase64) {
        byte[] audio = output.audioBytes();
        String encodedAudio = (includeAudioBase64 && audio != null && audio.length > 0)
                ? Base64.getEncoder().encodeToString(audio)
                : null;
        return new AssistantCommandResponse(
                output.interactionId(),
                output.transcribedText(),
                output.assistantResponse(),
                output.toolsInvoked(),
                encodedAudio,
                output.transcriptionLatencyMs(),
                output.reasoningLatencyMs(),
                output.speechLatencyMs(),
                output.totalLatencyMs(),
                output.createdAt()
        );
    }
}
