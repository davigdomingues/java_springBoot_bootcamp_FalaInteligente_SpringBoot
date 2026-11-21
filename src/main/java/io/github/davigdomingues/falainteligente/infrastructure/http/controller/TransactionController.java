package io.github.davigdomingues.falainteligente.infrastructure.http.controller;

import io.github.davigdomingues.falainteligente.application.input.TransactionFilter;
import io.github.davigdomingues.falainteligente.application.output.TransactionOutput;
import io.github.davigdomingues.falainteligente.application.usecase.CreateTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.DeleteTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetCategoryBreakdownUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetFinancialSummaryUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetMonthlyReportUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.GetTransactionUseCase;
import io.github.davigdomingues.falainteligente.application.usecase.ListTransactionsUseCase;
import io.github.davigdomingues.falainteligente.domain.enums.Category;
import io.github.davigdomingues.falainteligente.domain.enums.TransactionType;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.request.CreateTransactionRequest;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.ApiErrorResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.CategoryBreakdownResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.FinancialSummaryResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.MonthlyReportResponse;
import io.github.davigdomingues.falainteligente.infrastructure.http.dto.response.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Controlador REST fino para gestão de transações financeiras, resumos contábeis e relatórios mensais.
 *
 * Estrutura e impacto: atua como adaptador de entrada HTTP aplicando Bean Validation ({@code @Valid})
 * e delegando toda regra de negócio aos casos de uso da camada {@code application}. Erros de
 * validação sintática retornam HTTP {@code 400}, recursos ausentes {@code 404} e violações de
 * invariantes {@code 422} via {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "CRUD e relatórios analíticos do orçamento pessoal")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final GetFinancialSummaryUseCase getFinancialSummaryUseCase;
    private final GetCategoryBreakdownUseCase getCategoryBreakdownUseCase;
    private final GetMonthlyReportUseCase getMonthlyReportUseCase;

    public TransactionController(
            CreateTransactionUseCase createTransactionUseCase,
            ListTransactionsUseCase listTransactionsUseCase,
            GetTransactionUseCase getTransactionUseCase,
            DeleteTransactionUseCase deleteTransactionUseCase,
            GetFinancialSummaryUseCase getFinancialSummaryUseCase,
            GetCategoryBreakdownUseCase getCategoryBreakdownUseCase,
            GetMonthlyReportUseCase getMonthlyReportUseCase
    ) {
        this.createTransactionUseCase = Objects.requireNonNull(
                createTransactionUseCase, "createTransactionUseCase must not be null");
        this.listTransactionsUseCase = Objects.requireNonNull(
                listTransactionsUseCase, "listTransactionsUseCase must not be null");
        this.getTransactionUseCase = Objects.requireNonNull(
                getTransactionUseCase, "getTransactionUseCase must not be null");
        this.deleteTransactionUseCase = Objects.requireNonNull(
                deleteTransactionUseCase, "deleteTransactionUseCase must not be null");
        this.getFinancialSummaryUseCase = Objects.requireNonNull(
                getFinancialSummaryUseCase, "getFinancialSummaryUseCase must not be null");
        this.getCategoryBreakdownUseCase = Objects.requireNonNull(
                getCategoryBreakdownUseCase, "getCategoryBreakdownUseCase must not be null");
        this.getMonthlyReportUseCase = Objects.requireNonNull(
                getMonthlyReportUseCase, "getMonthlyReportUseCase must not be null");
    }

    @PostMapping
    @Operation(summary = "Create a financial transaction",
            description = "Registra uma nova despesa ou receita validada e persistida no banco relacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation or malformed payload error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Domain rule violation",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        TransactionOutput output = createTransactionUseCase.execute(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(output));
    }

    @GetMapping
    @Operation(summary = "List financial transactions with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Invalid date range filter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<TransactionResponse> response = listTransactionsUseCase
                .execute(new TransactionFilter(category, type, startDate, endDate))
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Invalid UUID format",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(TransactionResponse.from(getTransactionUseCase.execute(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction by UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction deleted"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Invalid UUID format",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> deleteTransaction(@PathVariable String id) {
        return ResponseEntity.ok(TransactionResponse.from(deleteTransactionUseCase.execute(id)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get consolidated financial summary (income, expenses, balance and average expense)")
    public ResponseEntity<FinancialSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                FinancialSummaryResponse.from(getFinancialSummaryUseCase.execute(startDate, endDate)));
    }

    @GetMapping("/summary/by-category")
    @Operation(summary = "Get expense breakdown grouped by category")
    public ResponseEntity<List<CategoryBreakdownResponse>> getCategoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CategoryBreakdownResponse> response = getCategoryBreakdownUseCase
                .execute(startDate, endDate)
                .stream()
                .map(CategoryBreakdownResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/monthly")
    @Operation(summary = "Get monthly consolidated financial report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly report generated"),
            @ApiResponse(responseCode = "400", description = "Missing year or month query parameter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Invalid year or month value",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(MonthlyReportResponse.from(getMonthlyReportUseCase.execute(year, month)));
    }
}
