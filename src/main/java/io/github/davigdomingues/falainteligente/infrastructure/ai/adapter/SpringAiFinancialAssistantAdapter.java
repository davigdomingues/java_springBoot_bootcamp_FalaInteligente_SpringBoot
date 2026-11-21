package io.github.davigdomingues.falainteligente.infrastructure.ai.adapter;

import io.github.davigdomingues.falainteligente.application.service.ToolInvocationContext;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Adaptador de saída Spring AI 2.0 que conecta {@link FinancialAssistantPort} ao {@link ChatClient}.
 *
 * Estrutura e impacto: injeta o parâmetro dinâmico {@code currentDate} no template
 * {@code classpath:prompts/system-message.st}, abre o escopo {@link ToolInvocationContext} com a
 * origem da chamada ({@code VOICE_AI} ou {@code TEXT_AI}) e deixa o {@code ToolCallingAdvisor}
 * conduzir o ciclo de Tool Calling até a resposta final.
 */
@Component
@Profile("!demo")
public class SpringAiFinancialAssistantAdapter implements FinancialAssistantPort {

    private final ChatClient chatClient;
    private final String systemPromptTemplate;
    private final Clock clock;
    private final OpenAiConfigurationGuard configurationGuard;

    public SpringAiFinancialAssistantAdapter(
            ChatClient chatClient,
            @Value("classpath:prompts/system-message.st") Resource systemPromptResource,
            Clock clock,
            OpenAiConfigurationGuard configurationGuard
    ) throws IOException {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        Objects.requireNonNull(systemPromptResource, "systemPromptResource must not be null");
        this.systemPromptTemplate = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.configurationGuard = Objects.requireNonNull(
                configurationGuard, "configurationGuard must not be null");
    }

    @Override
    public AssistantReply process(String userMessage, TransactionSource source) {
        configurationGuard.ensureConfigured();
        String currentDate = LocalDate.now(clock).toString();

        try (ToolInvocationContext.Scope scope = ToolInvocationContext.open(source)) {
            String response = chatClient.prompt()
                    .system(spec -> spec.text(systemPromptTemplate).param("currentDate", currentDate))
                    .user(userMessage)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                throw new AiProviderUnavailableException("Modelo de linguagem retornou resposta vazia");
            }
            return new AssistantReply(response.trim(), scope.invokedTools());
        } catch (DomainValidationException | ResourceNotFoundException | AiProviderUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiProviderUnavailableException(
                    "Falha ao processar comando no modelo de linguagem OpenAI: " + ex.getMessage(),
                    ex
            );
        }
    }
}
