package com.smartspend.copilot.integration.service;

import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.exception.TransactionNotFoundException;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest // 启动整个 SpringBoot application
@ActiveProfiles("test")
@Transactional // 每个 test 独立，每个 test 跑完自动 rollback database
public class TransactionServiceIntegrationTest {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @MockitoBean
    private AIService aiService;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private String usdDescription;
    private String vndDescription;

    private Transaction usdTransaction;
    private Transaction vndTransaction;

    @BeforeEach
    public void setUp(){
        usdDescription = "spent 15$ for pizza at Dominos";

        usdTransaction = new Transaction();
        usdTransaction.setAmount(15.0);
        usdTransaction.setCategory("Food");
        usdTransaction.setMerchant("Dominos");

        vndDescription = "Paid 240K dong for Grab ride";

        vndTransaction = new Transaction();
        vndTransaction.setAmount(240000.0);
        vndTransaction.setCategory("Transport");
        vndTransaction.setMerchant("Grab");
    }

    @Test
    void shouldProcessAndSaveUsdTransactionSuccessfully(){
        // Arrange
        when(aiService.parseTransaction(usdDescription)).thenReturn(usdTransaction);

        // Act
        Transaction savedTransaction = transactionService.processTransaction(usdDescription);

        // Assert
        // Check if it save successfully
        assertNotNull(savedTransaction.getId());

        // Business logic verification （check its currency)
        assertEquals("USD", savedTransaction.getCurrency());

        // database verification
        List<Transaction> databaseTransactions = transactionRepository.findAll();

        // check if database has exists new Transaction
        assertEquals(1, databaseTransactions.size());

        // 确认 persistence 正确
        assertEquals("Dominos", databaseTransactions.getFirst().getMerchant());
    }

    @Test
    void shouldConvertAndSaveVndTransactionSuccessfully(){
        // Arrange
        when(aiService.parseTransaction(vndDescription)).thenReturn(vndTransaction);
        when(exchangeRateService.getRate("USD", "VND")).thenReturn(24000.0);

        // Act
        Transaction savedTransaction = transactionService.processTransaction(vndDescription);

        // Assert
        // Verify the conversion
        assertEquals(10.0, savedTransaction.getAmount());

        // Verify the currency normalize
        assertEquals("USD", savedTransaction.getCurrency());

        // verify the original currency
        assertEquals("VND", savedTransaction.getOriginalCurrency());

        // database verification
        List<Transaction> databaseTransactions = transactionRepository.findAll();

        assertEquals(1, databaseTransactions.size());
        assertEquals("Grab", databaseTransactions.getFirst().getMerchant());
    }

    // check description is null or blank
    @Test
    void shouldThrowExceptionWhenDescriptionIsBlank(){
        // Act and Assert
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () ->
                {transactionService.processTransaction("");});

        assertEquals("Description cannot be blank", exception.getMessage());
    }

    // check AI service fails
    @Test
    void shouldThrowExceptionWhenAIParsingFails(){
        // Arrange
        when(aiService.parseTransaction(anyString())).thenReturn(null);

        // Act and Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () ->transactionService.processTransaction(usdDescription));
        assertEquals("Failed to parse transaction", exception.getMessage());
    }

    @Test
    void shouldDeleteTransactionSuccessfully(){
        // Arrange
        Transaction savedTransaction = transactionRepository.save(usdTransaction);

        // Act
        transactionService.deleteTransaction(savedTransaction.getId());

        // Assert
        boolean exist = transactionRepository.existsById(savedTransaction.getId());
        assertFalse(exist);

    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingTransaction(){
        Long id = 999L;
        // Act and Assert
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                        () -> transactionService.deleteTransaction(id));

        assertEquals("Transaction Not Found with ID: " + id, exception.getMessage());
    }

    @Test
    void shouldReturnAllTransactionsWhenNoFiltersProvided(){
        // Arrange
        transactionRepository.save(usdTransaction);
        transactionRepository.save(vndTransaction);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                null, null, "amount", "desc", 0, 10
        );

        // Assert
        assertEquals(2, transactions.getContent().size());
    }

    @Test
    void shouldReturnTransactionByCategoryWhenCategoryProvided(){
        // Arrange
        transactionRepository.save(usdTransaction);
        transactionRepository.save(vndTransaction);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                "food", null, "amount", "asc", 0, 10
        );

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals("Food", transactions.getContent().getFirst().getCategory());
    }

    @Test
    void shouldReturnTransactionByMerchantWhenMerchantProvided(){
        // Arrange
        transactionRepository.save(usdTransaction);
        transactionRepository.save(vndTransaction);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                null, "Grab", "amount", "desc", 0, 10
        );

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals("Grab", transactions.getContent().getFirst().getMerchant());
    }

    @Test
    void shouldReturnTransactionsByCategoryAndMerchantWhenCategoryAndMerchantProvided() {
        // Arrange
        transactionRepository.save(usdTransaction);
        transactionRepository.save(vndTransaction);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                "Food", "Dominos", "amount", "asc", 0, 10
        );

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals("Food", transactions.getContent().getFirst().getCategory());
        assertEquals("Dominos", transactions.getContent().getFirst().getMerchant());
    }
}
