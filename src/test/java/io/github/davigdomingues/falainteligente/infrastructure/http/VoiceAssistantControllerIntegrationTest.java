package io.github.davigdomingues.falainteligente.infrastructure.http;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;
import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort;
import io.github.davigdomingues.falainteligente.domain.port.FinancialAssistantPort.AssistantReply;
import io.github.davigdomingues.falainteligente.domain.port.SpeechToTextPort;
import io.github.davigdomingues.falainteligente.domain.port.TextToSpeechPort;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração HTTP dos controladores {@code VoiceAssistantController} e
 * {@code AuditController} utilizando {@link MockitoBean} nas portas de IA.
 *
 * Cobertura crítica: retorno binário {@code audio/mpeg} com headers de rastreabilidade,
 * telemetria detalhada em JSON, STT/TTS diretos, mapeamento de falhas ({@code 400}/{@code 415}/{@code 503})
 * e consolidação da trilha de auditoria e métricas em {@code /api/v1/audit/*}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoiceAssistantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataAiAuditRepository auditRepository;

    @MockitoBean
    private SpeechToTextPort speechToTextPort;

    @MockitoBean
    private FinancialAssistantPort financialAssistantPort;

    @MockitoBean
    private TextToSpeechPort textToSpeechPort;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void shouldProcessVoiceTextTranscribeAndSpeechEndpointsAndRecordAuditMetrics() throws Exception {
        byte[] audioInput = new byte[]{1, 2, 3, 4, 5};
        byte[] mp3Output = new byte[]{73, 68, 51, 3, 0, 1, 2, 3};

        when(speechToTextPort.transcribe(eq("comando.m4a"), any(), any()))
                .thenReturn("Gastei 85 reais no mercado hoje");
        when(financialAssistantPort.process("Gastei 85 reais no mercado hoje", TransactionSource.VOICE_AI))
                .thenReturn(new AssistantReply(
                        "Despesa de 85 reais registrada em Supermercado.",
                        List.of("register-transaction")
                ));
        when(textToSpeechPort.synthesize(any())).thenReturn(mp3Output);

        MockMultipartFile validFile = new MockMultipartFile(
                "file", "comando.m4a", "audio/mp4", audioInput);

        // 1. Endpoint principal /voice -> MP3 + headers de rastreabilidade
        MvcResult voiceAudioResult = mockMvc.perform(multipart("/api/v1/assistant/voice").file(validFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().exists("X-Interaction-Id"))
                .andExpect(header().string("X-Tools-Invoked", "register-transaction"))
                .andExpect(header().exists("X-Total-Latency-Ms"))
                .andExpect(content().bytes(mp3Output))
                .andReturn();

        String interactionId = voiceAudioResult.getResponse().getHeader("X-Interaction-Id");

        // 2. Endpoint /voice/details -> JSON com Base64 opcional
        mockMvc.perform(multipart("/api/v1/assistant/voice/details")
                        .file(validFile)
                        .param("includeAudioBase64", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcribedText").value("Gastei 85 reais no mercado hoje"))
                .andExpect(jsonPath("$.assistantResponse").value("Despesa de 85 reais registrada em Supermercado."))
                .andExpect(jsonPath("$.toolsInvoked[0]").value("register-transaction"))
                .andExpect(jsonPath("$.audioBase64", notNullValue()));

        // 3. Endpoint /text
        when(financialAssistantPort.process("Qual é o meu saldo?", TransactionSource.TEXT_AI))
                .thenReturn(new AssistantReply("Seu saldo líquido é positivo.", List.of("get-financial-summary")));

        mockMvc.perform(post("/api/v1/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Qual é o meu saldo?",
                                  "includeAudio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantResponse").value("Seu saldo líquido é positivo."))
                .andExpect(jsonPath("$.toolsInvoked[0]").value("get-financial-summary"));

        // 4. Endpoint /transcribe
        mockMvc.perform(multipart("/api/v1/assistant/transcribe").file(validFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Gastei 85 reais no mercado hoje"));

        // 5. Endpoint /speech
        mockMvc.perform(post("/api/v1/assistant/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Olá, teste de síntese."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(mp3Output));

        // 6. Consulta individual de auditoria e métricas consolidadas
        mockMvc.perform(get("/api/v1/audit/interactions/{id}", interactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("VOICE"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/audit/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInteractions").value(5))
                .andExpect(jsonPath("$.successfulInteractions").value(5))
                .andExpect(jsonPath("$.failedInteractions").value(0))
                .andExpect(jsonPath("$.successRatePercent").value(100.00))
                .andExpect(jsonPath("$.toolUsageCounts.register-transaction").value(2));
    }

    @Test
    void shouldReturn400_415_503And404AndAuditFailures() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.mp3", "audio/mpeg", new byte[0]);
        mockMvc.perform(multipart("/api/v1/assistant/voice").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Audio File"));

        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file", "script.txt", "text/plain", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/assistant/voice").file(unsupportedFile))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Audio Format"));

        when(speechToTextPort.transcribe(eq("ok.mp3"), any(), any()))
                .thenThrow(new AiProviderUnavailableException("OPENAI_API_KEY ausente"));
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "ok.mp3", "audio/mpeg", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/assistant/voice").file(validFile))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI Provider Unavailable"));

        mockMvc.perform(get("/api/v1/audit/interactions")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/v1/audit/interactions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
}
