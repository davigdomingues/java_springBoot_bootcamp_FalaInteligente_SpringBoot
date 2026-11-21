package io.github.davigdomingues.falainteligente.infrastructure.ai.demo;

import io.github.davigdomingues.falainteligente.domain.port.SpeechToTextPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Implementação determinística offline de {@link SpeechToTextPort} ativada no perfil {@code demo}.
 *
 * Estrutura e impacto: permite demonstrar e testar todo o pipeline de voz sem credenciais da OpenAI,
 * inferindo frases realistas em português brasileiro a partir de palavras-chave no nome do arquivo.
 */
@Component
@Profile("demo")
public class DemoSpeechToTextAdapter implements SpeechToTextPort {

    @Override
    public String transcribe(String filename, String contentType, byte[] audioBytes) {
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (normalized.contains("receita") || normalized.contains("salario")) {
            return "Recebi 5000 reais de salário hoje";
        }
        if (normalized.contains("resumo") || normalized.contains("saldo")) {
            return "Qual é o meu resumo financeiro?";
        }
        if (normalized.contains("categoria") || normalized.contains("ranking")) {
            return "Quanto gastei por categoria?";
        }
        if (normalized.contains("farmacia") || normalized.contains("remedio")) {
            return "Paguei 64,90 reais na farmácia com remédios hoje";
        }
        if (normalized.contains("listar") || normalized.contains("ultimas")) {
            return "Liste minhas últimas transações";
        }
        return "Gastei 85,50 reais no supermercado hoje";
    }
}
