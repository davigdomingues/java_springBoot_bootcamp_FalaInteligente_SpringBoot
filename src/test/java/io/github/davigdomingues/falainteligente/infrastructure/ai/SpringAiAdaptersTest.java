package io.github.davigdomingues.falainteligente.infrastructure.ai;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.infrastructure.ai.adapter.OpenAiConfigurationGuard;
import io.github.davigdomingues.falainteligente.infrastructure.ai.adapter.SpringAiFinancialAssistantAdapter;
import io.github.davigdomingues.falainteligente.infrastructure.ai.adapter.SpringAiSpeechToTextAdapter;
import io.github.davigdomingues.falainteligente.infrastructure.ai.adapter.SpringAiTextToSpeechAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários dos adaptadores de saída Spring AI 2.0 ({@link OpenAiConfigurationGuard},
 * {@link SpringAiSpeechToTextAdapter}, {@link SpringAiTextToSpeechAdapter} e
 * {@link SpringAiFinancialAssistantAdapter}).
 *
 * Cobertura crítica: preservação de {@code filename} no {@link ByteArrayResource} para o Whisper,
 * renderização de {@code currentDate} no prompt do sistema via {@link ChatClient} e conversão de
 * falhas de provedor em {@link AiProviderUnavailableException}.
 */
@ExtendWith(MockitoExtension.class)
class SpringAiAdaptersTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TranscriptionModel transcriptionModel;
    @Mock
    private TextToSpeechModel textToSpeechModel;
    @Mock
    private ChatModel chatModel;

    @Test
    void shouldValidateOpenAiConfigurationGuard() {
        assertThatThrownBy(() -> new OpenAiConfigurationGuard("").ensureConfigured())
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThatThrownBy(() -> new OpenAiConfigurationGuard("not-configured").ensureConfigured())
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThatCode(() -> new OpenAiConfigurationGuard("sk-valid-key").ensureConfigured())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPreserveFilenameWhenCallingTranscriptionModel() {
        OpenAiConfigurationGuard guard = new OpenAiConfigurationGuard("sk-valid-key");
        SpringAiSpeechToTextAdapter adapter = new SpringAiSpeechToTextAdapter(transcriptionModel, guard);
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("Paguei 50 reais");

        String transcript = adapter.transcribe("comando.m4a", "audio/mp4", new byte[]{1, 2, 3});

        assertThat(transcript).isEqualTo("Paguei 50 reais");
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(transcriptionModel).transcribe(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getFilename()).isEqualTo("comando.m4a");
    }

    @Test
    void shouldRejectEmptyAudioOrWrapProviderErrorOnTts() {
        OpenAiConfigurationGuard guard = new OpenAiConfigurationGuard("sk-valid-key");
        SpringAiTextToSpeechAdapter adapter = new SpringAiTextToSpeechAdapter(textToSpeechModel, guard);

        when(textToSpeechModel.call("ok")).thenReturn(new byte[]{1, 2, 3});
        assertThat(adapter.synthesize("ok")).containsExactly(1, 2, 3);

        when(textToSpeechModel.call("empty")).thenReturn(new byte[0]);
        assertThatThrownBy(() -> adapter.synthesize("empty"))
                .isInstanceOf(AiProviderUnavailableException.class);
    }

    @Test
    void shouldRenderCurrentDateInSystemPromptAndReturnReplyFromChatClient() throws IOException {
        OpenAiConfigurationGuard guard = new OpenAiConfigurationGuard("sk-valid-key");
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        Resource promptResource = new ByteArrayResource(
                "Assistente financeiro. Data: {currentDate}".getBytes(StandardCharsets.UTF_8));

        SpringAiFinancialAssistantAdapter adapter = new SpringAiFinancialAssistantAdapter(
                chatClient, promptResource, FIXED_CLOCK, guard);

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Registrado com sucesso."))))
        );

        AssistantReply reply = adapter.process("Gastei 30 reais", TransactionSource.TEXT_AI);

        assertThat(reply.responseText()).isEqualTo("Registrado com sucesso.");
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getSystemMessage().getText()).contains("2026-11-20");
        assertThat(promptCaptor.getValue().getUserMessage().getText()).contains("Gastei 30 reais");
    }
}
