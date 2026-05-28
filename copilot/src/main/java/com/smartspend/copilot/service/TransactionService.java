package com.smartspend.copilot.service;

import com.smartspend.copilot.exception.AIParsingException;
import com.smartspend.copilot.exception.TransactionNotFoundException;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AIService aiService;
    private final ExchangeRateService exchangeRateService;
    private final TransactionRepository transactionRepository;

    public Transaction processTransaction(String description){

        if(description == null || description.isBlank()){
            throw new IllegalArgumentException("Description cannot be blank");
        }
        // 1. determine whether the text was given in VND
        boolean isVnd = containsVndCurrency(description);

        // 2. pass raw string to AI to get structured data
        Transaction transaction = aiService.parseTransaction(description);
        if(transaction == null){
            throw new AIParsingException("Failed to parse transaction");
        }

        transaction.setOriginalDescription(description);

        // 3. normalize amounts to USD for storage if the prompt is in VND
        if(isVnd && transaction.getAmount() != null){
            double rate = exchangeRateService.getRate("USD", "VND");
            transaction.setAmount(transaction.getAmount()/rate);
            transaction.setOriginalCurrency("VND");
            transaction.setCurrency("USD");
        }else{
            transaction.setOriginalCurrency("USD");
            transaction.setCurrency("USD");
        }

        // 4. save to database
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id){
        if(!transactionRepository.existsById(id)){
            throw new TransactionNotFoundException("Transaction Not Found with ID: " + id);
        }
        transactionRepository.deleteById(id);
    }

    public Page<Transaction> getTransactions(
            String category, String merchant, String sort, String order, int page, int size
    ){
        // 预防以后会被其他object调用过去
        sort = (sort == null || sort.isBlank()) ? "amount" : sort;
        order = (order == null || order.isBlank()) ? "desc" : order;

        // sort ASC or DESC
        Sort.Direction direction = order.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // allowed sorting fields
        String sortField = switch (sort.toLowerCase()) {
            case "merchant"  -> "merchant";
            case "category" -> "category";
            case "id" -> "id";
            default -> "amount";
        };

        Sort sortConfig = Sort.by(direction, sortField);

        // 第几页, 每页几条, 排序规则
        Pageable pageable = PageRequest.of(page, size, sortConfig);

        // find by category and merchant
        if(category != null && !category.isBlank() && merchant != null && !merchant.isBlank() ){
            return transactionRepository.findByCategoryIgnoreCaseAndMerchantIgnoreCase(
                    category.trim(), merchant.trim(), pageable
            );
        }

        // find by category
        if(category != null &&  !category.isBlank() ){
            return transactionRepository.findByCategoryIgnoreCase(category.trim(), pageable);
        }

        // find by merchant
        if (merchant != null && !merchant.isBlank() ){
            return transactionRepository.findByMerchantIgnoreCase(merchant.trim(), pageable);
        }

        return transactionRepository.findAll(pageable);
    }

    private boolean containsVndCurrency(String description){
        String normalized = description.toLowerCase(Locale.ROOT);

        return normalized.contains("vnd") ||
            normalized.contains("đ") ||
            normalized.contains("dong") ||
            normalized.contains("d\\u00f4ng");

    }
}
