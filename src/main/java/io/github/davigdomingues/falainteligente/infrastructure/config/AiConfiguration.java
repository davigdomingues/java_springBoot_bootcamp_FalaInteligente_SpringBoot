package io.github.davigdomingues.falainteligente.infrastructure.config;

import io.github.davigdomingues.falainteligente.infrastructure.ai.tool.FinancialAssistantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuração do {@link ChatClient} do Spring AI 2.0 com registro explícito das ferramentas de
 * domínio para Tool Calling.
 *
 * Estrutura e impacto: ativa somente fora do perfil {@code demo}, acoplando
 * {@link FinancialAssistantTools} como ferramentas padrão para que o {@code ToolCallingAdvisor}
 * gerencie o loop completo de raciocínio e execução de casos de uso.
 */
@Configuration
@Profile("!demo")
public class AiConfiguration {

    @Bean
    public ChatClient financialAssistantChatClient(
            ChatClient.Builder chatClientBuilder,
            FinancialAssistantTools financialAssistantTools
    ) {
        return chatClientBuilder
                .defaultTools(financialAssistantTools)
                .build();
    }
}
