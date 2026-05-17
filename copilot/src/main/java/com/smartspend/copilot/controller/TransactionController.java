package com.smartspend.copilot.controller;

import com.smartspend.copilot.model.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final AIService aiService;
    private final TransactionRepository repository;
    private final ExchangeRateService exchangeRateService;

    @PostMapping("/process")
    public ResponseEntity<Transaction> processExpense(@RequestBody Map<String, String> payload){
        String description = payload.get("description");
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 1. determine whether the text was given in VND
        boolean isVnd = containsVndCurrency(description);

        // 2. pass raw string to AI to get structured data
        Transaction transaction = aiService.parseTransaction(description);
        transaction.setOriginalDescription(description);

        // 3. normalize amounts to USD for storage if the prompt is in VND
        if (isVnd && transaction.getAmount() != null) {
            double rate = exchangeRateService.getRate("USD", "VND");
            transaction.setAmount(transaction.getAmount() / rate);
            transaction.setOriginalCurrency("VND");
            transaction.setCurrency("USD");
        } else {
            transaction.setCurrency("USD");
            transaction.setOriginalCurrency("USD");
        }

        return ResponseEntity.ok(repository.save(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean containsVndCurrency(String description) {
        String normalized = description.toLowerCase(Locale.ROOT);
        return normalized.contains("vnd") || normalized.contains("đ") || normalized.contains("dong") || normalized.contains("d\u00f4ng");
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false, defaultValue = "amount") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order
    ){
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = switch (sort.toLowerCase()) {
            case "merchant" -> "merchant";
            case "category" -> "category";
            case "id" -> "id";
            default -> "amount";
        };
        Sort sortConfig = Sort.by(direction, sortField);

        List<Transaction> transactions;
        if (category != null && !category.isBlank() && merchant != null && !merchant.isBlank()) {
            transactions = repository.findByCategoryIgnoreCaseAndMerchantIgnoreCase(category.trim(), merchant.trim(), sortConfig);
        } else if (category != null && !category.isBlank()) {
            transactions = repository.findByCategoryIgnoreCase(category.trim(), sortConfig);
        } else if (merchant != null && !merchant.isBlank()) {
            transactions = repository.findByMerchantIgnoreCase(merchant.trim(), sortConfig);
        } else {
            transactions = repository.findAll(sortConfig);
        }

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam(required = false, defaultValue = "USD") String base,
            @RequestParam(required = false, defaultValue = "VND") String target
    ){
        if (!isSupportedPair(base, target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported currency pair"));
        }

        double rate = exchangeRateService.getRate(base, target);
        return ResponseEntity.ok(Map.of(
                "base", base.toUpperCase(),
                "target", target.toUpperCase(),
                "rate", rate,
                "timestamp", System.currentTimeMillis()
        ));
    }

    private boolean isSupportedPair(String base, String target) {
        return (base.equalsIgnoreCase("USD") && target.equalsIgnoreCase("VND"))
                || (base.equalsIgnoreCase("VND") && target.equalsIgnoreCase("USD"))
                || base.equalsIgnoreCase(target);
    }
}

