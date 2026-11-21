package io.github.davigdomingues.falainteligente.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da especificação OpenAPI 3 / Swagger UI da API Fala Inteligente.
 *
 * Estrutura e impacto: define os metadados institucionais e a visão arquitetural dos endpoints de
 * orçamento, pipeline multimodal de voz (STT -> ChatClient/Tools -> TTS) e trilha de auditoria
 * exibidos em {@code /swagger-ui.html} e {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI falaInteligenteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fala Inteligente - API de Assistente Financeiro por Voz")
                        .version("1.0.0")
                        .description("""
                                API inteligente de orçamento pessoal construída com Java 25, Spring Boot 4, \
                                Spring AI 2, Clean Architecture e DDD. Integra transcrição de áudio (STT), \
                                Tool Calling declarativo via ChatClient e síntese de voz (TTS) sem acoplar o \
                                domínio aos frameworks externos, com auditoria persistente e modo Demo offline.""")
                        .contact(new Contact()
                                .name("Davi Gabriel Domingues")
                                .url("https://github.com/davigdomingues"))
                        .license(new License()
                                .name("Licença MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
