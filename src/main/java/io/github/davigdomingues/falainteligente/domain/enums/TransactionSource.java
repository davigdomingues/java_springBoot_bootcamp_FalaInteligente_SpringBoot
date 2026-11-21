package io.github.davigdomingues.falainteligente.domain.enums;

/**
 * Origem de criação de uma transação financeira para fins de rastreabilidade e auditoria.
 *
 * Estrutura e impacto: permite distinguir lançamentos diretos via API REST daqueles disparados
 * autonomamente por ferramentas de Tool Calling a partir de comandos de voz ou texto.
 */
public enum TransactionSource {
    REST,
    VOICE_AI,
    TEXT_AI
}
