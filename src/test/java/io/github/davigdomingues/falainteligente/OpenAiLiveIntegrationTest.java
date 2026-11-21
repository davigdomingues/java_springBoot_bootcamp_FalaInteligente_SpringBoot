package io.github.davigdomingues.falainteligente;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração ao vivo contra a API da OpenAI (executado apenas quando
 * {@code OPENAI_API_KEY} estiver definida no ambiente).
 *
 * Cobertura crítica: valida Tool Calling real com {@code ChatClient} e geração de áudio MP3 real
 * com {@code TextToSpeechModel} no Spring AI 2.0.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OpenAiLiveIntegrationTest {

    @Autowired
    private FinancialAssistantPort financialAssistantPort;

    @Autowired
    private TextToSpeechPort textToSpeechPort;

    @Test
    void shouldExecuteLiveToolCallingAndTextToSpeechWhenApiKeyIsPresent() {
        AssistantReply reply = financialAssistantPort.process(
                "Registre uma despesa de 45 reais na farmácia hoje.",
                TransactionSource.TEXT_AI
        );

        assertThat(reply.responseText()).isNotBlank();
        assertThat(reply.toolsInvoked()).contains("register-transaction");

        byte[] audio = textToSpeechPort.synthesize(reply.responseText());
        assertThat(audio).hasSizeGreaterThan(1024);
    }
}
