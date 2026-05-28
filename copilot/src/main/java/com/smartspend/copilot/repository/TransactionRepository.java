package com.smartspend.copilot.repository;

import com.smartspend.copilot.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByCategoryIgnoreCase(String category, Pageable pageable);
    Page<Transaction> findByMerchantIgnoreCase(String merchant, Pageable pageable);
    Page<Transaction> findByCategoryIgnoreCaseAndMerchantIgnoreCase(String category, String merchant, Pageable pageable);
}
