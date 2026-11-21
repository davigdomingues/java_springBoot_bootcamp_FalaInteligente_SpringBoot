package io.github.davigdomingues.falainteligente.infrastructure.ai.demo;

import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Implementação determinística offline de {@link TextToSpeechPort} ativada no perfil {@code demo}.
 *
 * Estrutura e impacto: produz um payload MP3 sintético mínimo (cabeçalho ID3v2 + quadro MPEG-1 Layer 3)
 * para garantir respostas {@code audio/mpeg} válidas e testáveis sem chamadas remotas à OpenAI.
 */
@Component
@Profile("demo")
public class DemoTextToSpeechAdapter implements TextToSpeechPort {

    private static final byte[] SYNTHETIC_MP3_PAYLOAD = buildMinimalMp3Payload();

    @Override
    public byte[] synthesize(String text) {
        return SYNTHETIC_MP3_PAYLOAD.clone();
    }

    private static byte[] buildMinimalMp3Payload() {
        byte[] header = "ID3\u0003\u0000\u0000\u0000\u0000\u0000\u0000".getBytes(StandardCharsets.US_ASCII);
        byte[] frame = new byte[128];
        System.arraycopy(header, 0, frame, 0, header.length);
        // Sincronismo MPEG-1 Layer 3 (0xFF 0xFB) após o cabeçalho ID3 para compatibilidade com players
        frame[header.length] = (byte) 0xFF;
        frame[header.length + 1] = (byte) 0xFB;
        frame[header.length + 2] = (byte) 0x90;
        frame[header.length + 3] = (byte) 0x00;
        return frame;
    }
}
