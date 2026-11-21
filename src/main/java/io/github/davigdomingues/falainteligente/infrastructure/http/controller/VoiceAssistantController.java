package io.github.davigdomingues.falainteligente.infrastructure.http.controller;

import io.github.davigdomingues.falainteligente.application.input.AudioPayload;
import io.github.davigdomingues.falainteligente.application.output.AssistantCommandOutput;
import io.github.davigdomingues.falainteligente.application.usecase.ProcessTextCommandUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ProcessVoiceCommandUseCase;
import io.github.davigdomingues.falainteligente.domain.exception.InvalidAudioFileException;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.request.SpeechSynthesisRequest;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.request.TextCommandRequest;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.ApiErrorResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.AssistantCommandResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.TranscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * Controlador REST para os fluxos de Inteligência Artificial: comando por voz (áudio MP3 ou JSON
 * detalhado), comando em linguagem natural por texto, transcrição direta (STT) e síntese de voz (TTS).
 *
 * Estrutura e impacto: atua exclusivamente como adaptador HTTP fino sobre
 * {@link ProcessVoiceCommandUseCase} e {@link ProcessTextCommandUseCase}, anexando cabeçalhos de
 * rastreabilidade ({@code X-Interaction-Id}, {@code X-Tools-Invoked} e {@code X-Total-Latency-Ms})
 * nas respostas binárias de áudio.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@Tag(name = "Assistente de Voz (IA)", description = "Comandos financeiros por voz (STT -> Tool Calling -> TTS) e texto")
public class VoiceAssistantController {

    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");

    private final ProcessVoiceCommandUseCase processVoiceCommandUseCase;
    private final ProcessTextCommandUseCase processTextCommandUseCase;

    public VoiceAssistantController(
            ProcessVoiceCommandUseCase processVoiceCommandUseCase,
            ProcessTextCommandUseCase processTextCommandUseCase
    ) {
        this.processVoiceCommandUseCase = Objects.requireNonNull(
                processVoiceCommandUseCase, "processVoiceCommandUseCase must not be null");
        this.processTextCommandUseCase = Objects.requireNonNull(
                processTextCommandUseCase, "processTextCommandUseCase must not be null");
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mpeg")
    @Operation(
            summary = "Processar comando de voz e retornar resposta falada em MP3",
            description = "Recebe um áudio, transcreve com Whisper/STT, executa Tool Calling via ChatClient e devolve resposta falada em MP3."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta falada em MP3 gerada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo de áudio ausente ou vazio",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "Formato de áudio não suportado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Provedor OpenAI indisponível ou chave de API ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Resource> processVoiceAsAudio(@RequestParam("file") MultipartFile file) {
        AssistantCommandOutput output = processVoiceCommandUseCase.execute(toAudioPayload(file), true);
        ByteArrayResource resource = new ByteArrayResource(output.audioBytes());

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("assistant-response.mp3").build().toString())
                .header("X-Interaction-Id", output.interactionId().toString())
                .header("X-Tools-Invoked", String.join(",", output.toolsInvoked()))
                .header("X-Total-Latency-Ms", String.valueOf(output.totalLatencyMs()))
                .body(resource);
    }

    @PostMapping(
            value = "/voice/details",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Processar comando de voz e retornar telemetria estruturada em JSON",
            description = "Retorna transcrição, resposta do assistente, tools acionadas, latências e áudio Base64 opcional."
    )
    public ResponseEntity<AssistantCommandResponse> processVoiceAsJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "includeAudioBase64", defaultValue = "false") boolean includeAudioBase64
    ) {
        AssistantCommandOutput output = processVoiceCommandUseCase.execute(toAudioPayload(file), includeAudioBase64);
        return ResponseEntity.ok(AssistantCommandResponse.from(output, includeAudioBase64));
    }

    @PostMapping(
            value = "/text",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Processar comando financeiro em linguagem natural via texto",
            description = "Interpreta a mensagem em português brasileiro usando ChatClient + Tool Calling e registra auditoria."
    )
    public ResponseEntity<AssistantCommandResponse> processTextCommand(
            @Valid @RequestBody TextCommandRequest request
    ) {
        AssistantCommandOutput output = processTextCommandUseCase.execute(request.message(), request.includeAudio());
        return ResponseEntity.ok(AssistantCommandResponse.from(output, request.includeAudio()));
    }

    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Transcrever arquivo de áudio enviado para texto (Speech-to-Text)")
    public ResponseEntity<TranscriptionResponse> transcribeAudio(@RequestParam("file") MultipartFile file) {
        AssistantCommandOutput output = processVoiceCommandUseCase.transcribeOnly(toAudioPayload(file));
        return ResponseEntity.ok(TranscriptionResponse.from(output));
    }

    @PostMapping(
            value = "/speech",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "audio/mpeg"
    )
    @Operation(summary = "Sintetizar fala em MP3 a partir de texto (Text-to-Speech)")
    public ResponseEntity<Resource> synthesizeSpeech(@Valid @RequestBody SpeechSynthesisRequest request) {
        AssistantCommandOutput output = processTextCommandUseCase.synthesizeOnly(request.text());
        ByteArrayResource resource = new ByteArrayResource(output.audioBytes());

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("speech.mp3").build().toString())
                .header("X-Interaction-Id", output.interactionId().toString())
                .header("X-Total-Latency-Ms", String.valueOf(output.totalLatencyMs()))
                .body(resource);
    }

    private AudioPayload toAudioPayload(MultipartFile file) {
        try {
            return new AudioPayload(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException ex) {
            throw new InvalidAudioFileException("Unable to read uploaded audio file content");
        }
    }
}
