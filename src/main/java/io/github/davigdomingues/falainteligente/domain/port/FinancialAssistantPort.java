package io.github.davigdomingues.falainteligente.domain.port;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;

import java.util.List;
import java.util.Objects;

/**
 * Porta de saída para interpretação de intenção em linguagem natural e execução de ferramentas
 * de negócio via Tool Calling.
 *
 * Estrutura e impacto: isola o {@code ChatClient} e o loop de ferramentas do Spring AI 2.0 atrás
 * de um contrato simples, devolvendo tanto a resposta textual quanto a lista de ferramentas acionadas.
 */
public interface FinancialAssistantPort {

    AssistantReply process(String userMessage, TransactionSource source);

    /**
     * Resultado estruturado da etapa de raciocínio e execução de ferramentas do assistente.
     */
    record AssistantReply(
            String responseText,
            List<String> toolsInvoked
    ) {
        public AssistantReply {
            Objects.requireNonNull(responseText, "responseText must not be null");
            toolsInvoked = toolsInvoked == null ? List.of() : List.copyOf(toolsInvoked);
        }
    }
}
