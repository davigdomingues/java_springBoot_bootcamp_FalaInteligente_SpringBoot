package io.github.davigdomingues.falainteligente.domain.port;

/**
 * Porta de saída para síntese de voz a partir de texto (Text-to-Speech).
 *
 * Estrutura e impacto: abstrai o provedor de geração de áudio ({@code TextToSpeechModel} da OpenAI
 * ou adaptador determinístico no perfil {@code demo}).
 */
public interface TextToSpeechPort {

    byte[] synthesize(String text);
}
