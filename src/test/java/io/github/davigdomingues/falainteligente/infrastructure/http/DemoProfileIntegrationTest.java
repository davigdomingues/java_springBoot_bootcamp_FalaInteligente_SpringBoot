package io.github.davigdomingues.falainteligente.infrastructure.http;

import io.github.davigdomingues.falainteligente.infrastructure.persistence.repository.SpringDataAiAuditRepository;
import io.github.davigdomingues.falainteligente.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração ponta a ponta utilizando o perfil offline {@code demo} (sem mocks e sem
 * necessidade de {@code OPENAI_API_KEY}).
 *
 * Cobertura crítica: comprova que os adaptadores {@code Demo*} acionam as ferramentas reais
 * {@code FinancialAssistantTools}, persistem transações e registros de auditoria no banco H2 via
 * Flyway e devolvem áudio MP3 sintético funcional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
class DemoProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataAiAuditRepository auditRepository;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void shouldExecuteCompleteVoiceAndTextWorkflowsInOfflineDemoMode() throws Exception {
        // 1. Registrar receita via texto
        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Recebi 5000 reais de salário hoje",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolsInvoked[0]").value("register-transaction"))
                .andExpect(jsonPath("$.assistantResponse", containsString("Receita registrada")));

        // 2. Registrar despesa via voz (MP3 sintético + header X-Tools-Invoked)
        MockMultipartFile audioFile = new MockMultipartFile(
                "file", "compra-mercado.m4a", "audio/mp4", new byte[]{10, 20, 30, 40});
        mockMvc.perform(multipart("/api/v1/assistant/voice").file(audioFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("X-Tools-Invoked", "register-transaction"));

        // 3. Consultar resumo financeiro via texto
        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Qual é o meu resumo financeiro?",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolsInvoked[0]").value("get-financial-summary"))
                .andExpect(jsonPath("$.assistantResponse", containsString("5.000,00")));

        // 4. Consultar gastos por categoria via texto
        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Quanto gastei por categoria?",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolsInvoked[0]").value("get-category-spending-breakdown"))
                .andExpect(jsonPath("$.assistantResponse", containsString("Supermercado")));

        // 5. Verificar persistência real no H2
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 6. Verificar métricas consolidadas de auditoria
        mockMvc.perform(get("/api/v1/audit/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInteractions").value(4))
                .andExpect(jsonPath("$.successfulInteractions").value(4))
                .andExpect(jsonPath("$.successRatePercent").value(100.00))
                .andExpect(jsonPath("$.toolUsageCounts.register-transaction").value(2));
    }

    @Test
    void shouldParseBrazilianThousandsAndDecimalsCorrectlyInDemoMode() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Recebi 5.000 reais de salário",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantResponse", containsString("5.000,00")));

        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Gastei 1.250 reais de aluguel",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantResponse", containsString("1.250,00")));

        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Gastei 1.250,90 reais no supermercado",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantResponse", containsString("1.250,90")));

        mockMvc.perform(get("/api/v1/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(2500.90))
                .andExpect(jsonPath("$.balance").value(2499.10));
    }
}
