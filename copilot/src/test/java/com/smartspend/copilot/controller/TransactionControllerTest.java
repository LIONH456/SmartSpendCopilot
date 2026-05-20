package com.smartspend.copilot.controller;

import com.smartspend.copilot.model.Transaction;
import com.smartspend.copilot.repository.TransactionRepository;
import com.smartspend.copilot.service.AIService;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 只启动 web layer
@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    ObjectMapper objectMapper;

    // MockMVC: fake browser / fake HTTP client
    // @Autowired: 从 Spring 拿 bean
    @Autowired
    MockMvc mockMvc;

    // controller constructor needs  : because @RequiredArgsConstructor
    // Fake Spring Bean
    @MockitoBean
    ExchangeRateService exchangeRateService;

    @MockitoBean
    AIService aiService;

    @MockitoBean
    TransactionRepository transactionRepository;

    @MockitoBean
    TransactionService transactionService;

    private Transaction usdTransaction;
    private Transaction vndTransaction;
    private String usdDescription;
    private String vndDescription;
    private double rate;

    @BeforeEach
    void setUp() {
        usdDescription = "Spent 15 dollars on pizza";
        usdTransaction = new Transaction();
        usdTransaction.setAmount(15.0);
        usdTransaction.setCategory("Food");
        usdTransaction.setMerchant("Dominos");

        vndDescription = "Paid 240000VND for Grab ride to the airport";
        vndTransaction = new Transaction();
        vndTransaction.setAmount(240000.0);
        vndTransaction.setCategory("Transportation");
        vndTransaction.setMerchant("Grab");
    }

//    @Test
//    void shouldReturnExchangeRate() throws Exception{
//        // Arrange
//        when(exchangeRateService.getRate("USD", "VND")).thenReturn(26000.0);
//
//        // Act and Assert (andExpect)
//        mockMvc.perform(
//                get("/api/transactions/rate")
//                    .param("base", "USD")
//                    .param("target", "VND"))
//                    .andExpect(status().isOk()) // status 200
//                    .andExpect(jsonPath("$.base").value("USD"))
//                    .andExpect(jsonPath("$.target").value("VND"))
//                    .andExpect(jsonPath("$.rate").value(26000.0)//并且期待返回的 JSON 数据中，根目录下的 Rate 字段的值必须是 26000.0
//                    );
//    }
//
//    @Test
//    void shouldReturnBadRequestForUnsupportedCurrencyPair() throws Exception{
//        // Act and Assert
//        mockMvc.perform(
//                get("/api/transactions/rate")
//                .param("base", "EUR")
//                .param("target", "GBP"))
//                .andExpect(status().isBadRequest()) // status code 400
//                .andExpect(jsonPath("$.error").value("Unsupported currency pair")
//                );
//    }

//    @Test
//    void shouldProcessUsdTransactionSuccessfully() throws  Exception{
//        // Arrange: 模拟剧本（让 aiService 拦截并返回结果）
//        // 假装 AI 已经成功解析
//        when(aiService.parseTransaction(usdDescription)).thenReturn(usdTransaction);
//
//        // 假装数据库保存成功
//        when(transactionRepository.save(any(Transaction.class))).thenReturn(usdTransaction);
//
//        // 模拟前端发来的 JSON
//        // 核心技巧：用 Map.of 把描述包装起来
//        // 假设你的前端传参格式是： { "description": "Spent 15 dollars on pizza" }
//        Map<String, String> requestBody = Map.of("description", usdDescription);
//
//        // 核心工具：用 objectMapper 把 Map 转变成真正的 JSON 字符串文本 （java -> object)
//        String jsonRequest = objectMapper.writeValueAsString(requestBody);
//
//        // Act and Assert
//        mockMvc.perform(
//                post("/api/transactions/process")  // fake HTTP request
//                        .contentType(MediaType.APPLICATION_JSON) // 这个 request body 是 JSON
//                        .content(jsonRequest))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.amount").value(15.0))
//                    .andExpect(jsonPath("$.currency").value(usdTransaction.getCurrency()))
//                    .andExpect(jsonPath("$.merchant").value(usdTransaction.getMerchant()))
//                    .andExpect(jsonPath("$.category").value(usdTransaction.getCategory()))
//                    .andExpect(jsonPath("$.originalCurrency").value(usdTransaction.getOriginalCurrency())
//                    );
//        // 确认没调用过exchangeRateService
//        verify(exchangeRateService, never())
//                .getRate(anyString(), anyString());
//    }
//
//    @Test
//    void shouldProcessVndTransactionAndConvertToUsdTransactionSuccessfully() throws  Exception{
//        // Arrange
//        double rate = 24000;
//        when(exchangeRateService.getRate("USD", "VND")).thenReturn(rate);
//        when(aiService.parseTransaction(vndDescription)).thenReturn(vndTransaction);
//        when(transactionRepository.save(any(Transaction.class))).thenReturn(vndTransaction);
//
//        // 模拟前端发来的Json
//        Map<String,String> requestBody = Map.of("description", vndDescription);
//
//        // 把他变成真正的Json字符串
//        String jsonRequest = objectMapper.writeValueAsString(requestBody);
//
//        // Act and Assert
//        mockMvc.perform(
//                post("/api/transactions/process")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(jsonRequest)
//                ).andExpect(status().isOk())
//                .andExpect(jsonPath("$.amount").value(10.0))
//                .andExpect(jsonPath("$.currency").value(vndTransaction.getCurrency()))
//                .andExpect(jsonPath("$.merchant").value(vndTransaction.getMerchant()))
//                .andExpect(jsonPath("$.category").value(vndTransaction.getCategory()))
//                .andExpect(jsonPath("$.originalCurrency").value(vndTransaction.getOriginalCurrency()));
//        verify(exchangeRateService).getRate("USD", "VND");
//    }

//    @Test
//    void shouldDeleteTransactionSuccessfully() throws Exception{
//        Long id = 1L;
//        // Arrange
//        when(transactionRepository.existsById(id)).thenReturn(true);
//
//        // Act and Assert
//        mockMvc.perform(
//                delete(String.format("/api/transactions/%s", id)))
//                .andExpect(status().isNoContent());
//
//        // deleteById: return void (no value)
//        // verify if deleteById is called
//        verify(transactionRepository).deleteById(id);
//    }
//
//    @Test
//    void shouldReturnNotFoundWhenTransactionDoesNotExist() throws Exception{
//        // Arrange
//        Long id = 1L;
//        when(transactionRepository.existsById(id)).thenReturn(false);
//
//        // Act (perform the deletion) and Assert (expected to be not found)
//        mockMvc.perform(
//                delete(String.format("/api/transactions/%s", id)))
//                .andExpect(status().isNotFound());
//
//        // 确认deleteById没有被调用
//        verify(transactionRepository, never()).deleteById(id);
//    }
//
//    @Test
//    void shouldReturnAllTransactions() throws Exception {
//        // Arrange
//        List<Transaction> fakeTransactions = List.of(usdTransaction, vndTransaction);
//        when(transactionRepository.findAll(any(Sort.class))).thenReturn(fakeTransactions);
//
//        // Act and Assert
//        mockMvc.perform(
//                get("/api/transactions"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].amount").value(15.0))
//        .andExpect(jsonPath("$[0].currency").value(usdTransaction.getCurrency()));
//
//        verify(transactionRepository, times(1)).findAll(any(Sort.class));
//    }


}
