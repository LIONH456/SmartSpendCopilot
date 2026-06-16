package com.smartspend.copilot.integration.repository;

import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryIntegrationTest {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldSaveAndFindTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .amount(100.0)
                .category("Food")
                .merchant("Restaurant")
                .currency("USD")
                .originalCurrency("USD")
                .originalDescription("Lunch")
                .user(testUser)
                .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);
        Transaction found = transactionRepository.findById(saved.getId()).orElse(null);

        // Assert
        assertNotNull(saved);
        assertNotNull(found);
        assertEquals(saved.getAmount(), 100.0);
    }

    @Test
    void shouldFindTransactionByIdAndUser() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .amount(50.0)
                .category("Shopping")
                .merchant("Store")
                .currency("USD")
                .originalCurrency("USD")
                .user(testUser)
                .build();
        
        Transaction saved = transactionRepository.save(transaction);

        // Act
        Transaction found = transactionRepository.findByIdAndUser(saved.getId(), testUser).orElse(null);

        // Assert
        assertNotNull(found);
        assertEquals(found.getCategory(), "Shopping");
    }

    @Test
    void shouldFindTransactionsByUserWithPagination() {
        // Arrange
        for (int i = 0; i < 10; i++) {
            Transaction transaction = Transaction.builder()
                    .amount((double) (i * 10))
                    .category("Test")
                    .merchant("TestMerchant")
                    .currency("USD")
                    .originalCurrency("USD")
                    .user(testUser)
                    .build();
            transactionRepository.save(transaction);
        }

        // Act
        Page<Transaction> page = transactionRepository.findByUser(testUser, PageRequest.of(0, 5));

        // Assert
        assertEquals(page.getTotalElements(), 10);
        assertEquals(page.getSize(), 5);
        assertEquals(page.getNumber(), 0);
    }

    @Test
    void shouldFindTransactionsByUserAndCategory() {
        // Arrange
        Transaction foodTransaction = Transaction.builder()
                .amount(30.0)
                .category("Food")
                .merchant("FoodMart")
                .currency("USD")
                .originalCurrency("USD")
                .user(testUser)
                .build();
        
        Transaction otherTransaction = Transaction.builder()
                .amount(100.0)
                .category("Other")
                .merchant("OtherStore")
                .currency("USD")
                .originalCurrency("USD")
                .user(testUser)
                .build();
        
        transactionRepository.save(foodTransaction);
        transactionRepository.save(otherTransaction);

        // Act
        Page<Transaction> page = transactionRepository.findByUserAndCategoryIgnoreCase(
                testUser, "food", PageRequest.of(0, 10));

        // Assert
        assertEquals(page.getTotalElements(), 1);
        assertEquals(page.getContent().get(0).getCategory(), "Food");
    }

    @Test
    void shouldUpdateTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .amount(100.0)
                .category("OldCategory")
                .merchant("OldMerchant")
                .currency("USD")
                .originalCurrency("USD")
                .user(testUser)
                .build();
        
        Transaction saved = transactionRepository.save(transaction);

        // Act
        saved.setAmount(200.0);
        saved.setCategory("NewCategory");
        Transaction updated = transactionRepository.save(saved);

        // Assert
        assertEquals(updated.getAmount(), 200.0);
        assertEquals(updated.getCategory(), "NewCategory");
    }

    @Test
    void shouldDeleteTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .amount(50.0)
                .category("DeleteMe")
                .merchant("DeleteMerchant")
                .currency("USD")
                .originalCurrency("USD")
                .user(testUser)
                .build();
        
        Transaction saved = transactionRepository.save(transaction);

        // Act
        transactionRepository.delete(saved);

        // Assert
        assertFalse(transactionRepository.existsById(saved.getId()));
    }
}
