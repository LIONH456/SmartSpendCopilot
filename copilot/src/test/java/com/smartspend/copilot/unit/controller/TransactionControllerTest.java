package com.smartspend.copilot.unit.controller;

import com.smartspend.copilot.controller.TransactionController;
import com.smartspend.copilot.exception.AIParsingException;
import com.smartspend.copilot.exception.TransactionNotFoundException;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.service.ExchangeRateService;
import com.smartspend.copilot.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
    TransactionService transactionService;

    private Transaction usdTransaction;
    private Transaction vndTransaction;
    private String usdDescription;
    private String vndDescription;

    @BeforeEach
    void setUp() {
        usdDescription = "Spent 15 dollars on pizza";
        usdTransaction = new Transaction();
        usdTransaction.setAmount(15.0);
        usdTransaction.setCurrency("USD");
        usdTransaction.setOriginalCurrency("USD");
        usdTransaction.setCategory("Food");
        usdTransaction.setMerchant("Dominos");

        vndDescription = "Paid 240000VND for Grab ride to the airport";
        vndTransaction = new Transaction();
        vndTransaction.setAmount(10.0);
        vndTransaction.setCurrency("USD");
        vndTransaction.setOriginalCurrency("VND");
        vndTransaction.setCategory("Transportation");
        vndTransaction.setMerchant("Grab");
    }

    @Test
    void shouldProcessUsdTransactionSuccessfully() throws  Exception{
        // Arrange
        when(transactionService.processTransaction(usdDescription)).thenReturn(usdTransaction);

        // 模拟前端发来的 JSON
        // 核心技巧：用 Map.of 把描述包装起来
        // 假设你的前端传参格式是： { "description": "Spent 15 dollars on pizza" }
        Map<String, String> requestBody = Map.of("description", usdDescription);

        // 核心工具：用 objectMapper 把 Map 转变成真正的 JSON 字符串文本 （java -> object)
        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        // Act and Assert
        mockMvc.perform(
                post("/api/transactions/process")  // fake HTTP request
                        .contentType(MediaType.APPLICATION_JSON) // 这个 request body 是 JSON
                        .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(15.0))
                    .andExpect(jsonPath("$.currency").value(usdTransaction.getCurrency()))
                    .andExpect(jsonPath("$.merchant").value(usdTransaction.getMerchant()))
                    .andExpect(jsonPath("$.category").value(usdTransaction.getCategory()))
                    .andExpect(jsonPath("$.originalCurrency").value(usdTransaction.getOriginalCurrency())
                    );

        // Verify
        verify(transactionService).processTransaction(usdDescription);
    }

    @Test
    void shouldProcessVndTransactionSuccessfully() throws Exception{
        // Arrange
        when(transactionService.processTransaction(vndDescription)).thenReturn(vndTransaction);

        // 模拟前端发来的Json
        Map<String, String> requestBody = Map.of("description", vndDescription);

        // 把它转成Json格式，为了post给HTTP
        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        // Act and Assert
        mockMvc.perform(
                post("/api/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10.0))
                .andExpect(jsonPath("$.currency").value(vndTransaction.getCurrency()))
                .andExpect(jsonPath("$.merchant").value(vndTransaction.getMerchant()))
                .andExpect(jsonPath("$.category").value(vndTransaction.getCategory()))
                .andExpect(jsonPath("$.originalCurrency").value(vndTransaction.getOriginalCurrency()));

        // Verify
        verify(transactionService).processTransaction(vndDescription);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsBlank() throws Exception{
        // Arrange
        // 模拟前端发来请求
        Map<String, String> requestBody = Map.of("description", "");
        String jsonRequest = objectMapper.writeValueAsString(requestBody); // 把它转换成jsonString

        // Act and Assert
        mockMvc.perform(
                post("/api/transactions/process")
                    .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Description cannot be blank"));

        // Verify: 报错了就不会去掉service layer了
        verify(transactionService, never()).processTransaction(anyString());
    }

    // Service Throw Exception -> Bad Request
    @Test
    void shouldReturnBadRequestWhenTransactionServiceThrowException() throws Exception{
        // Arrange
        String description = "Spent 15 dollars";
        when(transactionService.processTransaction(anyString())).thenThrow(
                new AIParsingException("Failed to parse transaction"));

        // 模拟前端发来的请求
        Map<String, String> requestBody = Map.of("description", description);
        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        // Act and Assert
        mockMvc.perform(
                post("/api/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxServerError()); // internal Server Errors: 500

        // Verify Transaction is called but failed
         verify(transactionService).processTransaction(anyString());
    }

    @Test
    void shouldReturnAllTransactionsSuccessfully() throws Exception{
        // Arrange
        List<Transaction> fakeTransactions = List.of(usdTransaction,vndTransaction);
        when(transactionService
                .getTransactions(isNull(), isNull(), eq("amount"), eq("desc")))
                .thenReturn(fakeTransactions);

        // Act and Assert
        mockMvc.perform(
                get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant").value("Dominos"))
                .andExpect(jsonPath("$[1].merchant").value(vndTransaction.getMerchant()));

        // Verify
        verify(transactionService).getTransactions(isNull(), isNull(), eq("amount"), eq("desc"));
    }

    @Test
    void shouldReturnExchangeRate() throws Exception{
        // Arrange
        when(exchangeRateService.getRate("USD", "VND")).thenReturn(26000.0);

        // Act and Assert (andExpect)
        mockMvc.perform(
                get("/api/transactions/rate")
                        .param("base", "USD")
                        .param("target", "VND"))
                    .andExpect(status().isOk()) // status 200
                    .andExpect(jsonPath("$.base").value("USD"))
                    .andExpect(jsonPath("$.target").value("VND"))
                    .andExpect(jsonPath("$.rate").value(26000.0)//并且期待返回的 JSON 数据中，根目录下的 Rate 字段的值必须是 26000.0
                    );

        // Verify
        verify(exchangeRateService).getRate("USD", "VND");
    }

    @Test
    void shouldReturnBadRequestForUnsupportedCurrencyPair() throws Exception{
        // Arrange
        when(exchangeRateService.getRate("EUR", "GBP"))
                .thenThrow(new IllegalArgumentException("Unsupported currency pair"));
        // Act and Assert
        mockMvc.perform(
                get("/api/transactions/rate")
                    .param("base", "EUR")
                    .param("target", "GBP"))
                .andExpect(status().isBadRequest()) // status code 400
                .andExpect(jsonPath("$.error").value("Unsupported currency pair")
                );

        // Verify
        verify(exchangeRateService).getRate("EUR", "GBP");
    }

    @Test
    void shouldDeleteTransactionSuccessfully() throws Exception{
        // Arrange
        Long id = 1L;

        // Act and Assert
        mockMvc.perform(
                delete(String.format("/api/transactions/%s", id)))
                .andExpect(status().isNoContent());

        // deleteById: return void (no value)
        // verify if deleteById is called
        verify(transactionService).deleteTransaction(id);
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist() throws Exception{
        // Arrange
        Long id = 1L;

        // 对于返回值是 void 的方法，Mockito 规定必须把顺序反过来，使用 doThrow().when() 语法
        // deleteTransaction(id) 方法的返回值应该是 void
        doThrow(new TransactionNotFoundException("Transaction does not exist"))
                .when(transactionService).deleteTransaction(id);

        // Act (perform the deletion) and Assert (expected to be not found)
        mockMvc.perform(
                delete(String.format("/api/transactions/%s", id)))
                .andExpect(status().isNotFound());

        // 确认deleteById没有被调用
        verify(transactionService).deleteTransaction(id);
    }
}
