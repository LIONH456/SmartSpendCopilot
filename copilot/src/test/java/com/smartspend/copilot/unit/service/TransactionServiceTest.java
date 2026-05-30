package com.smartspend.copilot.unit.service;

import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;

import static org.junit.jupiter.api.Assertions.*;

import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private AIService aiService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction usdTransaction;
    private Transaction vndTransaction;

    private String usdDescription;
    private String vndDescription;

    @BeforeEach
    public void setUp(){

        usdTransaction = new Transaction();
        vndTransaction = new Transaction();

        usdDescription = "Spent 15 dollars on pizza";
        vndDescription = "Paid 240000 VND for Grab ride";

        usdTransaction.setAmount(15.0);
        usdTransaction.setCategory("Food");
        usdTransaction.setMerchant("Dominos");

        vndTransaction.setAmount(240000.0);
        vndTransaction.setCategory("Transport");
        vndTransaction.setMerchant("Grab");
    }

    @Test
    void shouldProcessUsdTransactionSuccessfully(){
        // Arrange
        when(aiService.parseTransaction(usdDescription)).thenReturn(usdTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(usdTransaction);

        // Act
        Transaction transaction = transactionService.processTransaction(usdDescription);

        // Assert
        assertEquals(usdTransaction.getCurrency(), transaction.getCurrency());
        assertEquals("USD", transaction.getOriginalCurrency());
        assertEquals(usdDescription, transaction.getOriginalDescription());
        assertEquals(15.0, transaction.getAmount());

        // verify
        verify(aiService).parseTransaction(usdDescription);
        verify(exchangeRateService, never()).getRate(anyString(), anyString());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldConvertVndTransactionToUsdTransactionSuccessfully(){
        // Arrange
        double rate = 24000.0;
        when(exchangeRateService.getRate("USD", "VND")).thenReturn(rate);
        when(aiService.parseTransaction(vndDescription)).thenReturn(vndTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(vndTransaction);

        // Act
        Transaction transaction = transactionService.processTransaction(vndDescription);

        // Assert
        assertEquals("USD", transaction.getCurrency());
        assertEquals("VND", transaction.getOriginalCurrency());
        assertEquals(10, transaction.getAmount());

        // Verify
        verify(aiService).parseTransaction(vndDescription);
        verify(exchangeRateService, times(1)).getRate("USD", "VND");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsBlank(){
        // Arrange
        String description = "";

        // Act
        AppException exception = assertThrows(
                AppException.class,
                () -> transactionService.processTransaction(description));

        // Assert
        assertEquals("Description cannot be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAIParsingFails(){
        // Arrange
        when(aiService.parseTransaction(anyString())).thenReturn(null);

        // Act
        AppException exception = assertThrows(
                AppException.class,
                () -> transactionService.processTransaction("Spend 15$ dollars")
        );

        // Assert
        assertEquals("Failed to parse transaction", exception.getMessage());

        // verify
        verify(aiService).parseTransaction("Spend 15$ dollars");
    }

    @Test
    void shouldDeleteTransactionWhenTransactionExists(){
        // Arrange
        Long id = 1L;
        when(transactionRepository.existsById(id)).thenReturn(true);

        // Act
        transactionService.deleteTransaction(id);

        // Assert and Verify
        verify(transactionRepository).deleteById(id);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingTransaction(){
        // Arrange
        Long id = 1L;
        when(transactionRepository.existsById(id)).thenReturn(false);

        // Act
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> transactionService.deleteTransaction(id)
//        );
        AppException exception = assertThrows(
                AppException.class,
                () -> transactionService.deleteTransaction(id)
        );

        // Assert
        assertEquals("Transaction Not Found with ID: 1", exception.getMessage());

        // Verify : 确认deleteById没有被调用
        verify(transactionRepository, never()).deleteById(id);
    }

    @Test
    void shouldReturnAllTransactionsWhenNoFiltersProvided(){
        // Arrange
        List<Transaction> fakeTransactions = List.of(usdTransaction, vndTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(fakeTransactions);

        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(transactionPage);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                null, null, "amount", "desc", 0, 10);

        // Assert
        assertEquals(2, transactions.getContent().size());
        assertEquals(fakeTransactions, transactions.getContent());

        // Verify: 看看是否被叫其他没必要的dependencies
        verify(transactionRepository, never()).findByCategoryIgnoreCase(anyString(), any());
        verify(transactionRepository, never())
                .findByCategoryIgnoreCaseAndMerchantIgnoreCase(anyString(), anyString(), any());
        verify(transactionRepository, never()).findByMerchantIgnoreCase(anyString(), any());

        // 确保findAll()被调用过
        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void shouldReturnTransactionsByCategoryWhenCategoryProvided(){
        // Arrange
        List<Transaction> fakeTransactions = List.of(usdTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(fakeTransactions);

        when(transactionRepository.findByCategoryIgnoreCase(eq("Food"), any()))
                .thenReturn(transactionPage);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                "Food", null, "amount", "desc", 0, 10);

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals(usdTransaction.getCategory(), transactions.getContent().getFirst().getCategory());

        // Verify: 检查是否正确被调用
        verify(transactionRepository).findByCategoryIgnoreCase(eq("Food"), any());

        verify(transactionRepository, never())
                .findByCategoryIgnoreCaseAndMerchantIgnoreCase(anyString(), any(), any());
        verify(transactionRepository, never()).findByMerchantIgnoreCase(anyString(), any());
        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void shouldReturnTransactionsByMerchantWhenMerchantProvided(){
        // Arrange
        Page<Transaction> transactionPage = new PageImpl<>(List.of(vndTransaction));
        when(transactionRepository.findByMerchantIgnoreCase(eq("Grab"), any(Pageable.class)))
                .thenReturn(transactionPage);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
            null, "Grab", "amount", "desc", 0, 10);

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals(List.of(vndTransaction), transactions.getContent());

        // verify
        verify(transactionRepository).findByMerchantIgnoreCase(eq("Grab"), any());

        verify(transactionRepository, never()).findByCategoryIgnoreCase(anyString(), any());
        verify(transactionRepository, never()).findByCategoryIgnoreCaseAndMerchantIgnoreCase(anyString(), anyString(), any());
        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void shouldReturnTransactionsByCategoryAndMerchantWhenBothProvided(){
        // Arrange
        Page <Transaction> transactionPage = new PageImpl<>(List.of(usdTransaction));
        when(transactionRepository
                .findByCategoryIgnoreCaseAndMerchantIgnoreCase(eq("Food"), eq("Dominos"), any()))
                .thenReturn(transactionPage);

        // Act
        Page<Transaction> transactions = transactionService.getTransactions(
                "Food", "Dominos", "amount", "desc", 0, 10);

        // Assert
        assertEquals(1, transactions.getContent().size());
        assertEquals(List.of(usdTransaction), transactions.getContent());

        // Verify
        verify(transactionRepository)
                .findByCategoryIgnoreCaseAndMerchantIgnoreCase(eq("Food"), eq("Dominos"), any());

        // 确认没被调用过
        verify(transactionRepository, never()).findAll(any(Pageable.class));
        verify(transactionRepository, never()).findByMerchantIgnoreCase(anyString(), any());
        verify(transactionRepository, never()).findByCategoryIgnoreCase(anyString(), any());

    }
}
