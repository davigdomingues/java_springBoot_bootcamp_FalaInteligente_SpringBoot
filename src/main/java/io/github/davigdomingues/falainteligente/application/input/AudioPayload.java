package io.github.davigdomingues.falainteligente.application.input;

/**
 * Representação em memória de um arquivo de áudio recebido para processamento por voz.
 *
 * Estrutura e impacto: preserva o nome original (com extensão) exigido por provedores como o
 * OpenAI Whisper e isola a camada de aplicação do tipo {@code MultipartFile} do Spring Web.
 */
public record AudioPayload(
        String filename,
        String contentType,
        byte[] bytes
) {

    public AudioPayload {
        bytes = bytes == null ? null : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes == null ? null : bytes.clone();
    }

    public long sizeBytes() {
        return bytes == null ? 0L : bytes.length;
    }
}
