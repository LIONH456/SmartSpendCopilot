package com.smartspend.copilot.controller;

import com.smartspend.copilot.model.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
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
    public ResponseEntity<Transaction> processExpense(@RequestBody Map<String, String> payload){
        String description = payload.get("description");
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(transactionService.processTransaction(description));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
        try{
            transactionService.deleteTransaction(id);
            log.info("you have deleted transaction with id {}", id);
            return ResponseEntity.noContent().build();
        }catch (IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        }
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
        try {
            double rate = exchangeRateService.getRate(base, target);
            return ResponseEntity.ok(
                    Map.of(
                            "base", base.toUpperCase(),
                            "target", target.toUpperCase(),
                            "rate", rate,
                            "timestamp", System.currentTimeMillis()
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())
            );
        }
    }
}

