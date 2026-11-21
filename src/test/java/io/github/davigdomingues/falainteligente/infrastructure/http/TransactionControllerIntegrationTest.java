package io.github.davigdomingues.falainteligente.infrastructure.http;

import com.openai.models.audio.AudioResponseFormat;
import io.github.davigdomingues.falainteligente.infrastructure.persistence.repository.SpringDataAiAuditRepository;
import io.github.davigdomingues.falainteligente.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração ponta a ponta de {@code TransactionController}, Flyway, JPA/H2,
 * OpenAPI e Actuator.
 *
 * Cobertura crítica: CRUD completo via REST, filtros por categoria e período, cálculo de resumo
 * financeiro, breakdown por categoria, relatório mensal, tratamento padronizado de erros
 * {@code 400}/{@code 404}/{@code 422} e publicação dos endpoints {@code /v3/api-docs} e
 * {@code /actuator/health}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataAiAuditRepository auditRepository;

    @Autowired
    private OpenAiChatProperties openAiChatProperties;

    @Autowired
    private OpenAiAudioTranscriptionProperties openAiAudioTranscriptionProperties;

    @Autowired
    private OpenAiAudioSpeechProperties openAiAudioSpeechProperties;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void shouldExecuteFullTransactionLifecycleSummaryBreakdownAndMonthlyReport() throws Exception {
        String salaryPayload = """
                {
                  "description": "Salário mensal",
                  "amount": 5000.00,
                  "type": "INCOME",
                  "category": "SALARY",
                  "transactionDate": "2026-11-05"
                }
                """;

        MvcResult salaryResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salaryPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Salário mensal"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.source").value("REST"))
                .andReturn();

        JsonNode salaryJson = objectMapper.readTree(salaryResult.getResponse().getContentAsString());
        String salaryId = salaryJson.get("id").asString();

        String groceriesPayload = """
                {
                  "description": "Compras no supermercado",
                  "amount": 300.00,
                  "type": "EXPENSE",
                  "category": "GROCERIES",
                  "transactionDate": "2026-11-10"
                }
                """;

        MvcResult groceriesResult = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groceriesPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String groceriesId = objectMapper.readTree(groceriesResult.getResponse().getContentAsString())
                .get("id").asString();

        String pharmaPayload = """
                {
                  "description": "Farmácia central",
                  "amount": 100.00,
                  "type": "EXPENSE",
                  "category": "PHARMA",
                  "transactionDate": "2026-11-15"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pharmaPayload))
                .andExpect(status().isCreated());

        // Listagem filtrada por categoria
        mockMvc.perform(get("/api/v1/transactions").param("category", "GROCERIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Compras no supermercado"));

        // Busca por ID
        mockMvc.perform(get("/api/v1/transactions/{id}", salaryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SALARY"));

        // Resumo financeiro consolidado
        mockMvc.perform(get("/api/v1/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(400.00))
                .andExpect(jsonPath("$.balance").value(4600.00))
                .andExpect(jsonPath("$.averageExpense").value(200.00))
                .andExpect(jsonPath("$.totalCount").value(3));

        // Detalhamento por categoria ordenado
        mockMvc.perform(get("/api/v1/transactions/summary/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$[0].percentageOfTotal").value(75.00))
                .andExpect(jsonPath("$[1].category").value("PHARMA"))
                .andExpect(jsonPath("$[1].percentageOfTotal").value(25.00));

        // Relatório mensal
        mockMvc.perform(get("/api/v1/transactions/reports/monthly")
                        .param("year", "2026")
                        .param("month", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(11))
                .andExpect(jsonPath("$.summary.balance").value(4600.00))
                .andExpect(jsonPath("$.transactions", hasSize(3)));

        // Exclusão e verificação de 404 após remoção
        mockMvc.perform(delete("/api/v1/transactions/{id}", groceriesId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groceriesId));

        mockMvc.perform(get("/api/v1/transactions/{id}", groceriesId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void shouldReturnStandardized400And422AndExposeOpenApiAndActuator() throws Exception {
        String invalidPayload = """
                {
                  "description": " ",
                  "amount": 0.00,
                  "type": null,
                  "category": null
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(get("/api/v1/transactions/id-invalido"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Domain Validation Error"));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("startDate", "2026-11-20")
                        .param("endDate", "2026-11-01"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Domain Validation Error"));

        mockMvc.perform(get("/api/v1/transactions/reports/monthly")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Domain Validation Error"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", containsString("Fala Inteligente")));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Regressão HTTP: rota inexistente (404), método não suportado (405) e media type inválido (415)
        mockMvc.perform(get("/api/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Endpoint Not Found"));

        mockMvc.perform(put("/api/v1/transactions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void shouldBindSpringAi2OpenAiFlatPropertiesWithoutDeprecatedOptionsPrefix() {
        assertThat(openAiChatProperties.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(openAiChatProperties.getTemperature()).isEqualTo(0.2);

        assertThat(openAiAudioTranscriptionProperties.getModel()).isEqualTo("whisper-1");
        assertThat(openAiAudioTranscriptionProperties.getLanguage()).isEqualTo("pt");
        assertThat(openAiAudioTranscriptionProperties.getTemperature()).isEqualTo(0.0f);
        assertThat(openAiAudioTranscriptionProperties.getResponseFormat()).isEqualTo(AudioResponseFormat.TEXT);

        assertThat(openAiAudioSpeechProperties.getModel()).isEqualTo("gpt-4o-mini-tts");
        assertThat(openAiAudioSpeechProperties.getVoice()).isEqualTo("nova");
        assertThat(openAiAudioSpeechProperties.getSpeed()).isEqualTo(1.1);
        assertThat(openAiAudioSpeechProperties.getResponseFormat()).isEqualTo("mp3");
    }
}
