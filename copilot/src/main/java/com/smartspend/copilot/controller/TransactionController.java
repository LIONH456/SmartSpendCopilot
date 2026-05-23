package com.smartspend.copilot.controller;

import com.smartspend.copilot.dto.request.ProcessTransactionRequest;
import com.smartspend.copilot.dto.response.TransactionResponse;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.mapper.TransactionMapper;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    private final ExchangeRateService exchangeRateService;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processExpense(@Valid @RequestBody ProcessTransactionRequest request){
        String description = request.getDescription();

        Transaction transaction = transactionService.processTransaction(description);

        TransactionResponse response = transactionMapper.toResponse(transaction);


            return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
            transactionService.deleteTransaction(id);
            log.info("you have deleted transaction with id {}", id);
            return ResponseEntity.noContent().build();

    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false, defaultValue = "amount") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order
    ){
        List<Transaction> transactions = transactionService.getTransactions(category, merchant, sort, order);

        // stream() :把 List 变成：“可操作的数据流” 类似：transaction1, transaction2, transaction3进入 pipeline
        // transactionMapper::toResponse: method reference = transaction -> transactionMapper.toResponse(transaction)
        // 把 transactions 列表里的每一个原始交易数据，排队通过转换器（Mapper）变成前端需要的格式，最后打包成一个新的列表 responses
        List<TransactionResponse> responses = transactions.stream().map(transactionMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
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

