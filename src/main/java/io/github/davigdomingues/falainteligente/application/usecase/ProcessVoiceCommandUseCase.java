package io.github.davigdomingues.falainteligente.application.usecase;

import io.github.davigdomingues.falainteligente.application.input.AudioPayload;
import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;
import io.github.davigdomingues.falainteligente.application.service.AiAuditRecorder;
import io.github.davigdomingues.falainteligente.application.service.AudioFileValidator;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.domain.port.SpeechToTextPort;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Caso de uso orquestrador do fluxo multimodal de comandos de voz (STT -> LLM/Tools -> TTS).
 *
 * Estrutura e impacto: coordena validação defensiva do arquivo de áudio, transcrição via
 * {@link SpeechToTextPort}, raciocínio e execução de funções reais via {@link FinancialAssistantPort},
 * síntese de resposta falada via {@link TextToSpeechPort} e gravação da auditoria completa
 * (inclusive em cenários de falha) via {@link AiAuditRecorder}.
 */
@Service
public class ProcessVoiceCommandUseCase {

    private final AudioFileValidator audioFileValidator;
    private final SpeechToTextPort speechToTextPort;
    private final FinancialAssistantPort financialAssistantPort;
    private final TextToSpeechPort textToSpeechPort;
    private final AiAuditRecorder aiAuditRecorder;

    public ProcessVoiceCommandUseCase(
            AudioFileValidator audioFileValidator,
            SpeechToTextPort speechToTextPort,
            FinancialAssistantPort financialAssistantPort,
            TextToSpeechPort textToSpeechPort,
            AiAuditRecorder aiAuditRecorder
    ) {
        this.audioFileValidator = Objects.requireNonNull(audioFileValidator, "audioFileValidator must not be null");
        this.speechToTextPort = Objects.requireNonNull(speechToTextPort, "speechToTextPort must not be null");
        this.financialAssistantPort = Objects.requireNonNull(
                financialAssistantPort, "financialAssistantPort must not be null");
        this.textToSpeechPort = Objects.requireNonNull(textToSpeechPort, "textToSpeechPort must not be null");
        this.aiAuditRecorder = Objects.requireNonNull(aiAuditRecorder, "aiAuditRecorder must not be null");
    }

    /**
     * Executa o pipeline ponta a ponta de voz, gerando resposta em texto e, opcionalmente, áudio MP3.
     */
    public AssistantCommandOutput execute(AudioPayload audio, boolean synthesizeAudio) {
        long pipelineStart = System.nanoTime();
        String filename = audio != null ? audio.filename() : null;
        Long audioSizeBytes = audio != null ? audio.sizeBytes() : null;
        long transcriptionLatencyMs = 0L;
        long reasoningLatencyMs = 0L;
        long speechLatencyMs = 0L;
        String transcribedText = null;
        String assistantResponse = null;
        List<String> toolsInvoked = List.of();

        try {
            audioFileValidator.validate(audio);

            long sttStart = System.nanoTime();
            String rawTranscript = speechToTextPort.transcribe(
                    audio.filename(),
                    audio.contentType(),
                    audio.bytes()
            );
            transcriptionLatencyMs = elapsedMillis(sttStart);

            if (rawTranscript == null || rawTranscript.isBlank()) {
                throw new DomainValidationException("Speech-to-text returned empty transcription");
            }
            transcribedText = rawTranscript.trim();

            long reasoningStart = System.nanoTime();
            AssistantReply reply = financialAssistantPort.process(transcribedText, TransactionSource.VOICE_AI);
            reasoningLatencyMs = elapsedMillis(reasoningStart);
            assistantResponse = reply.responseText();
            toolsInvoked = reply.toolsInvoked();

            byte[] synthesizedAudio = null;
            if (synthesizeAudio) {
                long speechStart = System.nanoTime();
                synthesizedAudio = textToSpeechPort.synthesize(assistantResponse);
                speechLatencyMs = elapsedMillis(speechStart);
            }

            long totalLatencyMs = elapsedMillis(pipelineStart);
            AiInteractionAudit audit = aiAuditRecorder.record(
                    InteractionChannel.VOICE,
                    filename,
                    audioSizeBytes,
                    transcribedText,
                    assistantResponse,
                    toolsInvoked,
                    InteractionStatus.SUCCESS,
                    null,
                    transcriptionLatencyMs,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    totalLatencyMs
            );

            return new AssistantCommandOutput(
                    audit.id(),
                    transcribedText,
                    assistantResponse,
                    toolsInvoked,
                    synthesizedAudio,
                    transcriptionLatencyMs,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    totalLatencyMs,
                    audit.createdAt()
            );
        } catch (RuntimeException ex) {
            aiAuditRecorder.record(
                    InteractionChannel.VOICE,
                    filename,
                    audioSizeBytes,
                    transcribedText,
                    assistantResponse,
                    toolsInvoked,
                    InteractionStatus.FAILED,
                    ex.getMessage(),
                    transcriptionLatencyMs,
                    reasoningLatencyMs,
                    speechLatencyMs,
                    elapsedMillis(pipelineStart)
            );
            throw ex;
        }
    }

    /**
     * Realiza apenas a transcrição do áudio enviado (STT) registrando auditoria em
     * {@link InteractionChannel#TRANSCRIPTION_ONLY}.
     */
    public AssistantCommandOutput transcribeOnly(AudioPayload audio) {
        long pipelineStart = System.nanoTime();
        String filename = audio != null ? audio.filename() : null;
        Long audioSizeBytes = audio != null ? audio.sizeBytes() : null;
        long transcriptionLatencyMs = 0L;
        String transcribedText = null;

        try {
            audioFileValidator.validate(audio);

            long sttStart = System.nanoTime();
            String rawTranscript = speechToTextPort.transcribe(
                    audio.filename(),
                    audio.contentType(),
                    audio.bytes()
            );
            transcriptionLatencyMs = elapsedMillis(sttStart);

            if (rawTranscript == null || rawTranscript.isBlank()) {
                throw new DomainValidationException("Speech-to-text returned empty transcription");
            }
            transcribedText = rawTranscript.trim();

            long totalLatencyMs = elapsedMillis(pipelineStart);
            AiInteractionAudit audit = aiAuditRecorder.record(
                    InteractionChannel.TRANSCRIPTION_ONLY,
                    filename,
                    audioSizeBytes,
                    transcribedText,
                    null,
                    List.of(),
                    InteractionStatus.SUCCESS,
                    null,
                    transcriptionLatencyMs,
                    0L,
                    0L,
                    totalLatencyMs
            );

            return new AssistantCommandOutput(
                    audit.id(),
                    transcribedText,
                    null,
                    List.of(),
                    null,
                    transcriptionLatencyMs,
                    0L,
                    0L,
                    totalLatencyMs,
                    audit.createdAt()
            );
        } catch (RuntimeException ex) {
            aiAuditRecorder.record(
                    InteractionChannel.TRANSCRIPTION_ONLY,
                    filename,
                    audioSizeBytes,
                    transcribedText,
                    null,
                    List.of(),
                    InteractionStatus.FAILED,
                    ex.getMessage(),
                    transcriptionLatencyMs,
                    0L,
                    0L,
                    elapsedMillis(pipelineStart)
            );
            throw ex;
        }
    }

    private long elapsedMillis(long startNanos) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }
}
