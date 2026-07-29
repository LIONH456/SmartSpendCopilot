package com.smartspend.copilot.controller;

import com.smartspend.copilot.dto.request.ProcessTransactionRequest;
import com.smartspend.copilot.dto.response.ApiErrorResponse;
import com.smartspend.copilot.dto.response.PaginatedResponse;
import com.smartspend.copilot.dto.response.TransactionResponse;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.mapper.TransactionMapper;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name="Transaction API", description = "APIs for managing SmartSpend transactions")
public class TransactionController {
    private final ExchangeRateService exchangeRateService;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Operation(
            summary = "Process a transaction (LEGACY single-item)",
            description = "Backwards-compatible endpoint: converts raw text into exactly ONE structured transaction. For multi-action / batch processing use /process-batch instead."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI parsing failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processExpense(@Valid @RequestBody ProcessTransactionRequest request){
        String description = request.getDescription();
        Transaction transaction = transactionService.processTransaction(description);
        TransactionResponse response = transactionMapper.toResponse(transaction);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Process a transaction (BATCH / multi-action aware)",
            description = "Parses raw user text that may contain AND / OR / comma / newline-separated / multi-amount descriptions into 1..N transactions. ALWAYS returns a JSON array (length >= 1 on success)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions processed successfully (array length >= 1)"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, blank description, unparseable text, or unsupported currency",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI infrastructure parsing failure",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @PostMapping("/process-batch")
    public ResponseEntity<List<TransactionResponse>> processExpenseBatch(
            @Valid @RequestBody ProcessTransactionRequest request
    ) {
        String description = request.getDescription();
        List<Transaction> transactions = transactionService.processTransactions(description);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Delete transaction",
            description = "Delete a transaction by id (must belong to current authenticated user)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
            transactionService.deleteTransaction(id);
            log.info("you have deleted transaction with id {}", id);
            return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get paginated transactions (USER-SCOPED)",
            description = "Retrieve ONLY the currently authenticated user's transactions, with optional category/merchant filtering, sorting, and pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully inside PaginatedResponse wrapper")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false, defaultValue = "amount") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Page<Transaction> transactionPage = transactionService.getTransactions(
                category, merchant, sort, order, page, size
        );

        List<TransactionResponse> responses = transactionPage.getContent()
                .stream()
                .map(transactionMapper::toResponse)
                .toList();

        PaginatedResponse<TransactionResponse> response =
                PaginatedResponse.<TransactionResponse>builder()
                        .content(responses)
                        .page(transactionPage.getNumber())
                        .size(transactionPage.getSize())
                        .totalElements(transactionPage.getTotalElements())
                        .totalPages(transactionPage.getTotalPages())
                        .last(transactionPage.isLast())
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get exchange rate",
            description = "Retrieve currency exchange rate — only USD↔VND pairs are supported."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported currency pair",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam(required = false, defaultValue = "USD") String base,
            @RequestParam(required = false, defaultValue = "VND") String target
    ){
            double rate = exchangeRateService.getRate(base, target);
            return ResponseEntity.ok(
                    Map.of(
                            "base", base.toUpperCase(),
                            "target", target.toUpperCase(),
                            "rate", rate,
                            "timestamp", System.currentTimeMillis()
                    )
            );
    }
}
