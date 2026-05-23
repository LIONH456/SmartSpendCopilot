package com.smartspend.copilot.integration.repository;

import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // 真的存数据库，然后rollback
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

        // Act
        List<Transaction> result = transactionRepository.findByCategoryIgnoreCase
                ("food", Sort.by(Sort.Direction.DESC, "amount"));

        // Assert
        assertEquals(1, result.size()); // 确保只查出来 1 条，把别的分类（如 Grab）挡在外面
        assertEquals("Food", result.getFirst().getCategory()); // 确保查出来的确实是 Food 分类
        assertEquals("Dominos", result.getFirst().getMerchant());
    }

    @Test
    void shouldFindTransactionsByMerchantIgnoreCase(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        // Act
        List<Transaction> results = transactionRepository.findByMerchantIgnoreCase(
                "grab", Sort.by(Sort.Direction.DESC, "amount"));

        // Assert
        assertEquals(1, results.size()); // 确保只查出来 1 条，把别的Merchant（如 Dominos）挡在外面
        assertEquals("Transport", results.getFirst().getCategory());
        assertEquals("Grab", results.getFirst().getMerchant());
    }

    @Test
    void shouldFindTransactionsByCategoryAndMerchant(){
        // Arrange
        transactionRepository.save(foodTransaction);
        transactionRepository.save(transportTransaction);

        // Act
        List<Transaction> transactions = transactionRepository.findByCategoryIgnoreCaseAndMerchantIgnoreCase(
            "food", "dominos", Sort.by(Sort.Direction.DESC, "amount")
        );

        // Assert
        assertEquals(1, transactions.size());
        assertEquals("Food", transactions.getFirst().getCategory());
        assertEquals("Dominos", transactions.getFirst().getMerchant());
    }
}
