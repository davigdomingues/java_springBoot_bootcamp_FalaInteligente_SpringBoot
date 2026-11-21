package io.github.davigdomingues.falainteligente.infrastructure.http.exception;

import io.github.davigdomingues.falainteligente.domain.exception.AiProviderUnavailableException;
import io.github.davigdomingues.falainteligente.domain.exception.DomainValidationException;
import io.github.davigdomingues.falainteligente.domain.exception.InvalidAudioFileException;
import io.github.davigdomingues.falainteligente.domain.exception.ResourceNotFoundException;
import io.github.davigdomingues.falainteligente.domain.exception.UnsupportedAudioFormatException;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Interceptador global ({@link RestControllerAdvice}) responsável por traduzir exceções de
 * validação, domínio, upload de áudio, disponibilidade de IA e falhas internas em respostas
 * padronizadas {@link ApiErrorResponse}.
 *
 * Matriz de mapeamento HTTP e impacto arquitetural:
 * - {@code 400 Bad Request}: {@link MethodArgumentNotValidException},
 *   {@link HttpMessageNotReadableException}, {@link MethodArgumentTypeMismatchException},
 *   {@link MissingServletRequestPartException}, {@link MissingServletRequestParameterException},
 *   {@link MultipartException} e {@link InvalidAudioFileException}.
 * - {@code 404 Not Found}: {@link ResourceNotFoundException} e {@link NoResourceFoundException}.
 * - {@code 405 Method Not Allowed}: {@link HttpRequestMethodNotSupportedException}.
 * - {@code 406 Not Acceptable}: {@link HttpMediaTypeNotAcceptableException}.
 * - {@code 413 Content Too Large}: {@link MaxUploadSizeExceededException}.
 * - {@code 415 Unsupported Media Type}: {@link UnsupportedAudioFormatException} e
 *   {@link HttpMediaTypeNotSupportedException}.
 * - {@code 422 Unprocessable Content}: {@link DomainValidationException} (preservando a lista de
 *   violações no campo {@code details}).
 * - {@code 503 Service Unavailable}: {@link AiProviderUnavailableException}.
 * - {@code 500 Internal Server Error}: fallback genérico para {@link Exception}, registrando o
 *   stack trace apenas em log e devolvendo mensagem opaca.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more request fields failed validation",
                request.getRequestURI(),
                details
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class
    })
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        String error = ex instanceof MultipartException ? "Invalid Multipart Request" : "Malformed Request";
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                error,
                "Invalid request payload or parameter value",
                request.getRequestURI(),
                List.of(extractSafeMessage(ex))
        );
    }

    @ExceptionHandler(InvalidAudioFileException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAudioFile(
            InvalidAudioFileException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Audio File",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint Not Found",
                "The requested endpoint or static resource was not found",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_ACCEPTABLE,
                "Not Acceptable",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONTENT_TOO_LARGE,
                "Content Too Large",
                "Uploaded audio file exceeds maximum configured multipart size",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(UnsupportedAudioFormatException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedAudioFormat(
            UnsupportedAudioFormatException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Audio Format",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainValidation(
            DomainValidationException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Domain Validation Error",
                ex.getMessage(),
                request.getRequestURI(),
                ex.getDetails()
        );
    }

    @ExceptionHandler(AiProviderUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleAiProviderUnavailable(
            AiProviderUnavailableException ex,
            HttpServletRequest request
    ) {
        log.warn("AI provider unavailable uri={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI Provider Unavailable",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while handling request uri={}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected internal error occurred",
                request.getRequestURI(),
                List.of()
        );
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String extractSafeMessage(Exception ex) {
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        return root.getMessage() != null ? root.getMessage() : "Invalid request payload";
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            List<String> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                path,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
