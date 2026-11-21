package io.github.davigdomingues.falainteligente.domain.enums;

/**
 * Estado final de execução de uma interação com os recursos de Inteligência Artificial.
 *
 * Estrutura e impacto: permite calcular taxa de sucesso e rastrear falhas de provedores ou
 * entradas inválidas na tabela {@code ai_interaction_audit_logs}.
 */
public enum InteractionStatus {
    SUCCESS,
    FAILED
}
