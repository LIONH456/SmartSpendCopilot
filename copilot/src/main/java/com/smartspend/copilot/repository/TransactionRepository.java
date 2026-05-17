package com.smartspend.copilot.repository;

import com.smartspend.copilot.model.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCategoryIgnoreCase(String category, Sort sort);
    List<Transaction> findByMerchantIgnoreCase(String merchant, Sort sort);
    List<Transaction> findByCategoryIgnoreCaseAndMerchantIgnoreCase(String category, String merchant, Sort sort);
}
