package io.github.davigdomingues.falainteligente.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuração central de relógio ({@link Clock}) para criação determinística de datas e timestamps.
 *
 * Estrutura e impacto: permite fixar o tempo em testes unitários enquanto utiliza UTC em execução real.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
