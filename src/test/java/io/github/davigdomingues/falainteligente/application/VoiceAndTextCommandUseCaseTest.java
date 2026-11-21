package io.github.davigdomingues.falainteligente.application;

import io.github.davigdomingues.falainteligente.application.input.AudioPayload;
import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;
import io.github.davigdomingues.falainteligente.application.service.AiAuditRecorder;
import io.github.davigdomingues.falainteligente.application.service.AudioFileValidator;
import io.github.davigdomingues.falainteligente.application.usecase.ProcessTextCommandUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ProcessVoiceCommandUseCase;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionChannel;
import io.github.davigdomingues.falainteligente.domain.enums.InteractionStatus;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.exception.UnsupportedAudioFormatException;
import io.github.davigdomingues.falainteligente.domain.model.AiInteractionAudit;
import io.github.davigdomingues.falainteligente.domain.port.AiInteractionAuditRepository;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.domain.port.SpeechToTextPort;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários dos casos de uso multimodais {@link ProcessVoiceCommandUseCase} e
 * {@link ProcessTextCommandUseCase}.
 *
 * Cobertura crítica: fluxo completo STT -> LLM/Tools -> TTS com telemetria e garantia de registro
 * da auditoria em status {@code FAILED} quando qualquer estágio do pipeline falha.
 */
@ExtendWith(MockitoExtension.class)
class VoiceAndTextCommandUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SpeechToTextPort speechToTextPort;
    @Mock
    private FinancialAssistantPort financialAssistantPort;
    @Mock
    private TextToSpeechPort textToSpeechPort;

    private RecordingAuditRepository auditRepository;
    private ProcessVoiceCommandUseCase voiceUseCase;
    private ProcessTextCommandUseCase textUseCase;

    @BeforeEach
    void setUp() {
        auditRepository = new RecordingAuditRepository();
        AiAuditRecorder recorder = new AiAuditRecorder(auditRepository, FIXED_CLOCK);
        AudioFileValidator validator = new AudioFileValidator(FalaInteligenteProperties.defaults());
        voiceUseCase = new ProcessVoiceCommandUseCase(
                validator, speechToTextPort, financialAssistantPort, textToSpeechPort, recorder);
        textUseCase = new ProcessTextCommandUseCase(
                financialAssistantPort, textToSpeechPort, recorder);
    }

    @Test
    void shouldExecuteVoicePipelineWithSpeechSynthesisAndPersistSuccessAudit() {
        byte[] audioInput = new byte[]{1, 2, 3, 4};
        byte[] audioOutput = new byte[]{9, 8, 7};
        when(speechToTextPort.transcribe("gasto.m4a", "audio/mp4", audioInput))
                .thenReturn("Gastei 80 reais no mercado");
        when(financialAssistantPort.process("Gastei 80 reais no mercado", TransactionSource.VOICE_AI))
                .thenReturn(new AssistantReply("Despesa de 80 reais registrada.", List.of("register-transaction")));
        when(textToSpeechPort.synthesize("Despesa de 80 reais registrada."))
                .thenReturn(audioOutput);

        AssistantCommandOutput result = voiceUseCase.execute(
                new AudioPayload("gasto.m4a", "audio/mp4", audioInput), true);

        assertThat(result.transcribedText()).isEqualTo("Gastei 80 reais no mercado");
        assertThat(result.assistantResponse()).isEqualTo("Despesa de 80 reais registrada.");
        assertThat(result.toolsInvoked()).containsExactly("register-transaction");
        assertThat(result.audioBytes()).isEqualTo(audioOutput);

        assertThat(auditRepository.saved).hasSize(1);
        AiInteractionAudit audit = auditRepository.saved.getFirst();
        assertThat(audit.channel()).isEqualTo(InteractionChannel.VOICE);
        assertThat(audit.status()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(audit.originalFilename()).isEqualTo("gasto.m4a");
    }

    @Test
    void shouldSkipSpeechSynthesisWhenFlagIsFalseAndSupportTranscribeOnly() {
        byte[] audioInput = new byte[]{5, 6, 7};
        when(speechToTextPort.transcribe("consulta.mp3", "audio/mpeg", audioInput))
                .thenReturn("Qual meu saldo?");
        when(financialAssistantPort.process("Qual meu saldo?", TransactionSource.VOICE_AI))
                .thenReturn(new AssistantReply("Seu saldo é de 1000 reais.", List.of("get-financial-summary")));

        AssistantCommandOutput voiceDetails = voiceUseCase.execute(
                new AudioPayload("consulta.mp3", "audio/mpeg", audioInput), false);
        assertThat(voiceDetails.audioBytes()).isNull();
        verifyNoInteractions(textToSpeechPort);

        AssistantCommandOutput transcriptionOnly = voiceUseCase.transcribeOnly(
                new AudioPayload("consulta.mp3", "audio/mpeg", audioInput));
        assertThat(transcriptionOnly.transcribedText()).isEqualTo("Qual meu saldo?");
        assertThat(auditRepository.saved.getLast().channel()).isEqualTo(InteractionChannel.TRANSCRIPTION_ONLY);
    }

    @Test
    void shouldPersistFailedAuditWhenSttLlmOrFormatValidationFails() {
        assertThatThrownBy(() -> voiceUseCase.execute(
                new AudioPayload("malware.exe", "application/octet-stream", new byte[]{1, 2}), true))
                .isInstanceOf(UnsupportedAudioFormatException.class);
        assertThat(auditRepository.saved.getLast().status()).isEqualTo(InteractionStatus.FAILED);

        when(speechToTextPort.transcribe(eq("falha.wav"), any(), any()))
                .thenThrow(new AiProviderUnavailableException("OpenAI offline"));
        assertThatThrownBy(() -> voiceUseCase.execute(
                new AudioPayload("falha.wav", "audio/wav", new byte[]{1, 2, 3}), true))
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThat(auditRepository.saved.getLast().status()).isEqualTo(InteractionStatus.FAILED);
        assertThat(auditRepository.saved.getLast().errorMessage()).contains("OpenAI offline");

        assertThatThrownBy(() -> textUseCase.execute("   ", false))
                .isInstanceOf(DomainValidationException.class);
        assertThat(auditRepository.saved.getLast().channel()).isEqualTo(InteractionChannel.TEXT);
        assertThat(auditRepository.saved.getLast().status()).isEqualTo(InteractionStatus.FAILED);
    }

    @Test
    void shouldProcessTextCommandAndSpeechOnlyWithAuditTrail() {
        when(financialAssistantPort.process("Resumo financeiro", TransactionSource.TEXT_AI))
                .thenReturn(new AssistantReply("Saldo positivo de 500 reais.", List.of("get-financial-summary")));
        when(textToSpeechPort.synthesize("Olá mundo")).thenReturn(new byte[]{10, 20});

        AssistantCommandOutput textResult = textUseCase.execute("Resumo financeiro", false);
        assertThat(textResult.assistantResponse()).isEqualTo("Saldo positivo de 500 reais.");

        AssistantCommandOutput speechResult = textUseCase.synthesizeOnly("Olá mundo");
        assertThat(speechResult.audioBytes()).containsExactly(10, 20);
        assertThat(auditRepository.saved.getLast().channel()).isEqualTo(InteractionChannel.SPEECH_ONLY);
    }

    private static final class RecordingAuditRepository implements AiInteractionAuditRepository {
        private final List<AiInteractionAudit> saved = new ArrayList<>();

        @Override
        public AiInteractionAudit save(AiInteractionAudit audit) {
            saved.add(audit);
            return audit;
        }

        @Override
        public Optional<AiInteractionAudit> findById(UUID id) {
            return saved.stream().filter(item -> item.id().equals(id)).findFirst();
        }

        @Override
        public List<AiInteractionAudit> findAll(InteractionStatus status, InteractionChannel channel) {
            return saved;
        }
    }
}
