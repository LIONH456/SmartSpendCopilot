package com.smartspend.copilot.service;

import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TransactionService {
    AIService aiService;
    ExchangeRateService exchangeRateService;
    TransactionRepository transactionRepository;
    CurrentUserService currentUserService;

    /**
     * LEGACY single-transaction entry point. Preserves backwards compatibility
     * with callers that expect exactly one Transaction. Internally delegates to
     * processTransactions() and returns the first persisted item.
     */
    public Transaction processTransaction(String description) {
        List<Transaction> all = processTransactions(description);
        return all.get(0);
    }

    /**
     * PRIMARY: parses a description into 1..N transactions via AI + regex fallback,
     * normalizes VND amounts to USD via ExchangeRateService (real-time API + cache +
     * 25000 fallback chain), binds to the currently authenticated user, and saves all.
     *
     * AMOUNT RULE (see AIService for upstream 0-free allowance):
     *   - amount == 0  →  ALLOWED (free / 薅羊毛 / 商家免单)
     *   - amount < 0   →  BLOCKED  (never saves negative values)
     */
    public List<Transaction> processTransactions(String description) {
        if (description == null || description.isBlank()) {
            throw new AppException(ErrorCode.DESCRIPTION_BLANK);
        }

        // 1. Determine whether the text was given in VND (đ / VND / dong)
        boolean isVnd = containsVndCurrency(description);

        // 2. Parse description via AI + regex fallback pipeline (already USD/VND-only)
        List<Transaction> parsed = aiService.parseTransactions(description);
        if (parsed == null || parsed.isEmpty()) {
            throw new AppException(ErrorCode.AI_PARSING_FAILED);
        }

        User currentUser = currentUserService.getCurrentUser();

        // =====================================================================
        // 3. EXCHANGE RATE PIPELINE:  API priority  →  1440-min cache  →  25000 fallback
        //    ExchangeRateService.getRate() already implements the 3-layer chain:
        //      L1: CACHE (if valid, instant return)
        //      L2: API fetch via ExchangeRateClient (open.er-api.com)
        //      L3: ABSOLUTE FALLBACK constant 25000 (when API throws / unavailable)
        //    Here we wrap in an ADDITIONAL try/catch safety net so that even a
        //    misconfiguration in ExchangeRateService can NEVER crash the main flow.
        // =====================================================================
        double rate;
        if (isVnd) {
            try {
                // Real-time API → cache (TTL 1440min) → hardcoded 25000 as last resort
                rate = exchangeRateService.getRate("USD", "VND");
            } catch (Exception ex) {
                log.error("ExchangeRate pipeline threw unexpected; applying ABSOLUTE fallback 25000", ex);
                rate = 25000.0;
            }
            if (rate <= 0) {
                log.warn("Exchange rate pipeline returned non-positive {}; applying fallback 25000", rate);
                rate = 25000.0;
            }
        } else {
            rate = 1.0;
        }

        List<Transaction> toSave = new ArrayList<>(parsed.size());
        for (Transaction tx : parsed) {
            // ---- AMOUNT INTEGRITY (FINAL GATE BEFORE DB): allow 0, block negative ----
            Double amount = tx.getAmount();
            if (amount == null || amount < 0) {
                log.warn("Dropping transaction with invalid amount={} from batch", amount);
                continue;
            }
            tx.setOriginalDescription(description);
            if (isVnd) {
                tx.setAmount(amount / rate); // VND → USD for storage
                tx.setOriginalCurrency("VND");
                tx.setCurrency("USD");
            } else {
                tx.setOriginalCurrency("USD");
                tx.setCurrency("USD");
            }
            // Bind to authenticated user (this is the column that makes per-user isolation work)
            tx.setUser(currentUser);
            toSave.add(tx);
        }

        if (toSave.isEmpty()) {
            throw new AppException(ErrorCode.AI_PARSING_FAILED);
        }

        // 4. Batch save: one INSERT statement per list item when using saveAll()
        return transactionRepository.saveAll(toSave);
    }

    public void deleteTransaction(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        transactionRepository.delete(transaction);
    }

    public Page<Transaction> getTransactions(
            String category, String merchant, String sort, String order, int page, int size
    ) {
        sort = (sort == null || sort.isBlank()) ? "amount" : sort;
        order = (order == null || order.isBlank()) ? "desc" : order;

        Sort.Direction direction = order.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String sortField = switch (sort.toLowerCase()) {
            case "merchant"  -> "merchant";
            case "category" -> "category";
            case "id" -> "id";
            default -> "amount";
        };

        Sort sortConfig = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortConfig);

        User currentUser = currentUserService.getCurrentUser();

        // All queries are user-scoped: SQL always appends WHERE user_id = :userId
        if (category != null && !category.isBlank() && merchant != null && !merchant.isBlank() ) {
            return transactionRepository.findByUserAndCategoryIgnoreCaseAndMerchantIgnoreCase(
                    currentUser, category.trim(), merchant.trim(), pageable
            );
        }
        if (category != null &&  !category.isBlank() ) {
            return transactionRepository.findByUserAndCategoryIgnoreCase(currentUser, category.trim(), pageable);
        }
        if (merchant != null && !merchant.isBlank() ) {
            return transactionRepository.findByUserAndMerchantIgnoreCase(currentUser, merchant.trim(), pageable);
        }
        return transactionRepository.findByUser(currentUser, pageable);
    }

    private boolean containsVndCurrency(String description) {
        String normalized = description.toLowerCase(Locale.ROOT);
        return normalized.contains("vnd") ||
            normalized.contains("đ") ||
            normalized.contains("dong") ||
            normalized.contains("d\u00f4ng");
    }
}
