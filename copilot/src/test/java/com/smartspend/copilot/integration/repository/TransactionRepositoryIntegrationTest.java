package com.smartspend.copilot.integration.repository;

import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // 真的存数据库，然后rollback
@ActiveProfiles("test")
public class TransactionRepositoryIntegrationTest {
    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction foodTransaction;
    private Transaction transportTransaction;

    @BeforeEach
    void setUp() {
        foodTransaction = new Transaction();

        foodTransaction.setAmount(15.0);
        foodTransaction.setCategory("Food");
        foodTransaction.setMerchant("Dominos");

        transportTransaction = new Transaction();
        transportTransaction.setAmount(15.0);
        transportTransaction.setCategory("Transport");
        transportTransaction.setMerchant("Grab");
    }

    @Test
    void shouldSaveAndRetrieveTransactionSuccessfully(){
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setAmount(15.0);
        transaction.setCategory("Food");
        transaction.setMerchant("Dominos");
        transaction.setCurrency("USD");
        transaction.setOriginalCurrency("USD");
        transaction.setOriginalDescription("Spent 15 dollars for pizza at Dominos");

        // Act
        Transaction savedTransaction = transactionRepository.save(transaction);
        List<Transaction> transactions = transactionRepository.findAll();

        // Assert
        // 确认数据库真的生成 primary key 了
        assertNotNull(savedTransaction.getId());

        // 确认真的 insert 成功了
        assertEquals(1, transactions.size());

        // 确认retrieve 出来的数据正确
        assertEquals("Dominos", transactions.get(0).getMerchant());
    }

    @Test
    void shouldFindTransactionsByCategoryIgnoreCase(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "amount"));

        // Act
        Page<Transaction> result = transactionRepository.findByCategoryIgnoreCase
                ("food", pageable);

        // Assert
        assertEquals(1, result.getContent().size()); // 确保只查出来 1 条，把别的分类（如 Grab）挡在外面
        assertEquals("Food", result.getContent().getFirst().getCategory()); // 确保查出来的确实是 Food 分类
        assertEquals("Dominos", result.getContent().getFirst().getMerchant());
    }

    @Test
    void shouldFindTransactionsByMerchantIgnoreCase(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "amount"));

        // Act
        Page<Transaction> results = transactionRepository.findByMerchantIgnoreCase(
                "grab", pageable);

        // Assert
        assertEquals(1, results.getContent().size()); // 确保只查出来 1 条，把别的Merchant（如 Dominos）挡在外面
        assertEquals("Transport", results.getContent().getFirst().getCategory());
        assertEquals("Grab", results.getContent().getFirst().getMerchant());
    }

    @Test
    void shouldFindTransactionsByCategoryAndMerchant(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "amount"));

        // Act
        Page<Transaction> transactions = transactionRepository.findByCategoryIgnoreCaseAndMerchantIgnoreCase(
            "food", "dominos", pageable
        );

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals("Food", transactions.getContent().getFirst().getCategory());
        assertEquals("Dominos", transactions.getContent().getFirst().getMerchant());
    }

    @Test
    void shouldReturnPaginatedTransactions(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        Pageable pageable = PageRequest.of(0, 1);

        // Act
        Page<Transaction> page = transactionRepository.findAll(pageable);

        // Assert
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertFalse(page.isEmpty());
    }
}
