package com.smartspend.copilot.service;

import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionService {
    AIService aiService;
    ExchangeRateService exchangeRateService;
    TransactionRepository transactionRepository;
    CurrentUserService currentUserService;

    public Transaction processTransaction(String description){

        if(description == null || description.isBlank()){
            throw new AppException(ErrorCode.DESCRIPTION_BLANK);
        }
        // 1. determine whether the text was given in VND
        boolean isVnd = containsVndCurrency(description);

        // 2. pass raw string to AI to get structured data
        Transaction transaction = aiService.parseTransaction(description);
        if(transaction == null){
            throw new AppException(ErrorCode.AI_PARSING_FAILED);
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

        // get current user
        User currentUser = currentUserService.getCurrentUser();
        transaction.setUser(currentUser);

        // 4. save to database
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id){
        User currentUser = currentUserService.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() ->new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        transactionRepository.delete(transaction);
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

        // 找current User
        User currentUser = currentUserService.getCurrentUser();

        // find by category and merchant
        if(category != null && !category.isBlank() && merchant != null && !merchant.isBlank() ){
            return transactionRepository.findByUserAndCategoryIgnoreCaseAndMerchantIgnoreCase(
                    currentUser, category.trim(), merchant.trim(), pageable
            );
        }

        // find by category
        if(category != null &&  !category.isBlank() ){
            return transactionRepository.findByUserAndCategoryIgnoreCase(currentUser, category.trim(), pageable);
        }

        // find by merchant
        if (merchant != null && !merchant.isBlank() ){
            return transactionRepository.findByUserAndMerchantIgnoreCase(currentUser, merchant.trim(), pageable);
        }

        return transactionRepository.findByUser(currentUser, pageable);
    }

    private boolean containsVndCurrency(String description){
        String normalized = description.toLowerCase(Locale.ROOT);

        return normalized.contains("vnd") ||
            normalized.contains("đ") ||
            normalized.contains("dong") ||
            normalized.contains("d\\u00f4ng");

    }
}
