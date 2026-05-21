package com.smartspend.copilot.controller;

import com.smartspend.copilot.dto.request.ProcessTransactionRequest;
import com.smartspend.copilot.dto.response.TransactionResponse;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    private final ExchangeRateService exchangeRateService;
    private final TransactionService transactionService;

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processExpense(@Valid @RequestBody ProcessTransactionRequest request){
        String description = request.getDescription();

        Transaction transaction = transactionService.processTransaction(description);

        TransactionResponse response = TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .merchant(transaction.getMerchant())
                .currency(transaction.getCurrency())
                .originalCurrency(transaction.getOriginalCurrency())
                .originalDescription(transaction.getOriginalDescription())
                .build();


            return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
            transactionService.deleteTransaction(id);
            log.info("you have deleted transaction with id {}", id);
            return ResponseEntity.noContent().build();

    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false, defaultValue = "amount") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order
    ){
        return ResponseEntity.ok(
                transactionService.getTransactions(
                        category,
                        merchant,
                        sort,
                        order
                )
        );
    }

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

