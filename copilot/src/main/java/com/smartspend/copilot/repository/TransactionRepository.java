package com.smartspend.copilot.repository;

import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUser(User user, Pageable pageable);
    Optional<Transaction> findByIdAndUser(Long id, User user);
    Page<Transaction> findByUserAndCategoryIgnoreCase(User user, String category, Pageable pageable);
    Page<Transaction> findByUserAndMerchantIgnoreCase(User user, String merchant, Pageable pageable);
    Page<Transaction> findByUserAndCategoryIgnoreCaseAndMerchantIgnoreCase(User user, String category, String merchant, Pageable pageable);
}
