package io.github.davigdomingues.falainteligente.domain.port;

/**
 * Porta de saída para conversão de áudio em texto (Speech-to-Text).
 *
 * Estrutura e impacto: abstrai o provedor de transcrição ({@code TranscriptionModel} da OpenAI ou
 * adaptador simulado no perfil {@code demo}) garantindo substituição transparente.
 */
public interface SpeechToTextPort {

    String transcribe(String filename, String contentType, byte[] audioBytes);
}
