package io.github.davigdomingues.falainteligente.domain.enums;

/**
 * Canal de entrada/saída de uma interação registrada na trilha de auditoria de IA.
 *
 * Estrutura e impacto: segmenta métricas operacionais entre o pipeline multimodal completo
 * ({@link #VOICE}), comandos em linguagem natural ({@link #TEXT}) e chamadas isoladas de STT/TTS.
 */
public enum InteractionChannel {
    VOICE,
    TEXT,
    TRANSCRIPTION_ONLY,
    SPEECH_ONLY
}
