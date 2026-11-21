package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;
import io.github.davigdomingues.falainteligente.application.service.AiAuditRecorder;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Caso de uso para processamento de comandos financeiros em linguagem natural (texto) e síntese
 * direta de voz (Text-to-Speech) com registro completo de auditoria.
 */
@Service
public class ProcessTextCommandUseCase {

    private final FinancialAssistantPort financialAssistantPort;
    private final TextToSpeechPort textToSpeechPort;
    private final AiAuditRecorder aiAuditRecorder;

    public ProcessTextCommandUseCase(
            FinancialAssistantPort financialAssistantPort,
            TextToSpeechPort textToSpeechPort,
            AiAuditRecorder aiAuditRecorder
    ) {
        this.financialAssistantPort = Objects.requireNonNull(
                financialAssistantPort, "financialAssistantPort must not be null");
        this.textToSpeechPort = Objects.requireNonNull(textToSpeechPort, "textToSpeechPort must not be null");
        this.aiAuditRecorder = Objects.requireNonNull(aiAuditRecorder, "aiAuditRecorder must not be null");
    }

    /**
     * Interpreta uma instrução textual via assistente de IA (com Tool Calling) e opcionalmente
     * sintetiza a resposta em áudio MP3.
     */
    public AssistantCommandOutput execute(String commandText, boolean synthesizeAudio) {
        long pipelineStart = System.nanoTime();
        long reasoningLatencyMs = 0L;
        long speechLatencyMs = 0L;
        String normalizedText = commandText == null ? null : commandText.trim();
        String assistantResponse = null;
        List<String> toolsInvoked = List.of();

        try {
            if (normalizedText == null || normalizedText.isBlank()) {
                throw new DomainValidationException("Command text must not be blank");
            }

            long reasoningStart = System.nanoTime();
            AssistantReply reply = financialAssistantPort.process(normalizedText, TransactionSource.TEXT_AI);
            reasoningLatencyMs = elapsedMillis(reasoningStart);
            assistantResponse = reply.responseText();
            toolsInvoked = reply.toolsInvoked();

            byte[] audioBytes = null;
            if (synthesizeAudio) {
                long speechStart = System.nanoTime();
                audioBytes = textToSpeechPort.synthesize(assistantResponse);
                speechLatencyMs = elapsedMillis(speechStart);
            }

            long totalLatencyMs = elapsedMillis(pipelineStart);
            AiInteractionAudit audit = aiAuditRecorder.record(
                    InteractionChannel.TEXT,
                    null,
                    null,
                    normalizedText,
                    assistantResponse,
                    toolsInvoked,
                    InteractionStatus.SUCCESS,
                    null,
                    0L,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    totalLatencyMs
            );

            return new AssistantCommandOutput(
                    audit.id(),
                    normalizedText,
                    assistantResponse,
                    toolsInvoked,
                    audioBytes,
                    0L,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    totalLatencyMs,
                    audit.createdAt()
            );
        } catch (RuntimeException ex) {
            aiAuditRecorder.record(
                    InteractionChannel.TEXT,
                    null,
                    null,
                    normalizedText,
                    assistantResponse,
                    toolsInvoked,
                    InteractionStatus.FAILED,
                    ex.getMessage(),
                    0L,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    elapsedMillis(pipelineStart)
            );
            throw ex;
        }
    }

    /**
     * Converte diretamente um texto em áudio MP3 via {@link TextToSpeechPort} mantendo auditoria
     * no canal {@link InteractionChannel#SPEECH_ONLY}.
     */
    public AssistantCommandOutput synthesizeOnly(String text) {
        long pipelineStart = System.nanoTime();
        long speechLatencyMs = 0L;
        String normalizedText = text == null ? null : text.trim();

        try {
            if (normalizedText == null || normalizedText.isBlank()) {
                throw new DomainValidationException("Text for speech synthesis must not be blank");
            }

            long speechStart = System.nanoTime();
            byte[] audioBytes = textToSpeechPort.synthesize(normalizedText);
            speechLatencyMs = elapsedMillis(speechStart);

            long totalLatencyMs = elapsedMillis(pipelineStart);
            AiInteractionAudit audit = aiAuditRecorder.record(
                    InteractionChannel.SPEECH_ONLY,
                    null,
                    null,
                    null,
                    normalizedText,
                    List.of(),
                    InteractionStatus.SUCCESS,
                    null,
                    0L,
                    0L,
                    speechLatencyMs,
                    totalLatencyMs
            );

            return new AssistantCommandOutput(
                    audit.id(),
                    null,
                    normalizedText,
                    List.of(),
                    audioBytes,
                    0L,
                    0L,
                    speechLatencyMs,
                    totalLatencyMs,
                    audit.createdAt()
            );
        } catch (RuntimeException ex) {
            aiAuditRecorder.record(
                    InteractionChannel.SPEECH_ONLY,
                    null,
                    null,
                    null,
                    normalizedText,
                    List.of(),
                    InteractionStatus.FAILED,
                    ex.getMessage(),
                    0L,
                    0L,
                    speechLatencyMs,
                    elapsedMillis(pipelineStart)
            );
            throw ex;
        }
    }

    private long elapsedMillis(long startNanos) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }
}
