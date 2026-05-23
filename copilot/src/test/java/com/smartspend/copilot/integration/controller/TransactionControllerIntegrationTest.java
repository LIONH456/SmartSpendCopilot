package com.smartspend.copilot.integration.controller;

import com.smartspend.copilot.dto.request.ProcessTransactionRequest;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 启动整个 SpringBoot application。包括：controller, service, repository, database, bean, mapper, exception handler
@AutoConfigureMockMvc // Spring 自动给你 MockMvc 否则@Autowired MockMvc会失败。
@Transactional // 每个 test 自动 rollback database。
public class TransactionControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private AIService aiService;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private String usdDescription;
    private String vndDescription;
    private Transaction usdTransaction;
    private Transaction vndTransaction;

    @BeforeEach
    void setUp(){
        usdDescription = "Spent 15$ on Pizza at Dominos";
        vndDescription = "Paid 240K vnd for Grab";

        usdTransaction = new Transaction();
        usdTransaction.setAmount(15.0);
        usdTransaction.setCategory("Food");
        usdTransaction.setMerchant("Dominos");

        vndTransaction = new Transaction();
        vndTransaction.setAmount(240000.0);
        vndTransaction.setCategory("Transport");
        vndTransaction.setMerchant("Grab");
    }

    @Test
    void shouldProcessUsdTransactionSuccessfully() throws Exception {
        // Arrange
        when(aiService.parseTransaction(usdDescription)).thenReturn(usdTransaction);

        // 模拟前端发来请求
        ProcessTransactionRequest request = new ProcessTransactionRequest();
        request.setDescription(usdDescription);

        // 把他转换成Json String
        String json = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/transactions/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchant").value("Dominos"))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.amount").value(15.0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.originalCurrency").value("USD"));;

        // Database Verification
        List<Transaction> transactions = transactionRepository.findAll();

        assertEquals(1, transactions.size());
        assertEquals("Dominos", transactions.getFirst().getMerchant());

        verify(aiService).parseTransaction(usdDescription);
        verify(exchangeRateService, never()).getRate("USD", "VND");
    }

    @Test
    void shouldProcessVndTransactionSuccessfully() throws Exception {
        // Arrange
        when(aiService.parseTransaction(vndDescription)).thenReturn(vndTransaction);
        when(exchangeRateService.getRate("USD", "VND")).thenReturn(24000.0);

        // 模拟前端发来请求
        ProcessTransactionRequest request = new ProcessTransactionRequest();
        request.setDescription(vndDescription);
        // 把它转成Json String
        String json = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchant").value("Grab"))
                .andExpect(jsonPath("$.category").value("Transport"))
                .andExpect(jsonPath("$.amount").value(10.0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.originalCurrency").value("VND"));

        // Database Verification
        List<Transaction> databaseTransaction = transactionRepository.findAll();

        assertEquals(1, databaseTransaction.size());
        assertEquals("Grab", databaseTransaction.getFirst().getMerchant());

        // verify these methods is called
        verify(aiService).parseTransaction(vndDescription);
        verify(exchangeRateService).getRate("USD", "VND");
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsBlank() throws Exception {
        // Arrange
        ProcessTransactionRequest request = new ProcessTransactionRequest();
        request.setDescription("");
        String json = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());

        // Database Verification
        List<Transaction> databaseTransactions = transactionRepository.findAll();
        assertEquals(0, databaseTransactions.size());
    }

    // When AI Parsig Fails
    @Test
    void shouldReturnInternalServerErrorWhenTransactionServiceThrowException() throws Exception {
        // Arrange
        when(aiService.parseTransaction(anyString())).thenReturn(null);

        ProcessTransactionRequest request = new ProcessTransactionRequest();
        request.setDescription(usdDescription);
        String json = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                post("/api/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());

        // Database Verification
        assertTrue(transactionRepository.findAll().isEmpty());
    }

    @Test
    void shouldDeleteTransactionSuccessfully() throws Exception {
        // Arrange
        Transaction savedTransaction = transactionRepository.save(vndTransaction);

        // Act and Assert
        mockMvc.perform(delete("/api/transactions/" + savedTransaction.getId()))
                .andExpect(status().isNoContent());

        // Database Verification
        assertFalse(transactionRepository.existsById(savedTransaction.getId()));
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/transactions/" + 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllTransactionsSuccessfully() throws Exception{
        // Arrange
        transactionRepository.save(vndTransaction);
        transactionRepository.save(usdTransaction);

        // Act and Assert
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].merchant").exists())
                .andExpect(jsonPath("$[1].merchant").exists());

        // Database Verification
        assertEquals(2, transactionRepository.findAll().size());
    }

    @Test
    void shouldReturnTransactionsByCategory() throws Exception {
        // Arrange
        transactionRepository.save(vndTransaction);
        transactionRepository.save(usdTransaction);

        // Act and Assert
        mockMvc.perform(get("/api/transactions").param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Food"));
    }

    @Test
    void shouldReturnTransactionsByMerchant() throws Exception {
        // Arrange
        transactionRepository.save(vndTransaction);
        transactionRepository.save(usdTransaction);

        // Act and Assert
        mockMvc.perform(get("/api/transactions").param("merchant", "Grab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].merchant").value("Grab"));
    }

    @Test
    void shouldReturnTransactionsByCategoryAndMerchant() throws Exception {
        // Arrange
        transactionRepository.save(vndTransaction);
        transactionRepository.save(usdTransaction);

        // Act and Assert
        mockMvc.perform(get("/api/transactions")
                .param("category", "Food")
                .param("merchant", "Dominos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].merchant").value("Dominos"));
    }
}
