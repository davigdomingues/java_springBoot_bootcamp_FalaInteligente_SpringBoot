package io.github.davigdomingues.falainteligente;

import io.github.davigdomingues.falainteligente.infrastructure.config.FalaInteligenteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Ponto de entrada da API Fala Inteligente (Spring Boot 4 + Spring AI 2 + Clean Architecture).
 *
 * Estrutura e impacto: inicializa o contêiner Spring, habilita {@link FalaInteligenteProperties}
 * de forma imutável e registra o grafo de casos de uso, persistência Flyway/JPA e adaptadores
 * de voz e Tool Calling.
 */
@SpringBootApplication
@EnableConfigurationProperties(FalaInteligenteProperties.class)
public class FalaInteligenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(FalaInteligenteApplication.class, args);
    }
}
