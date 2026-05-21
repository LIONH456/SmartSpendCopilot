package com.smartspend.copilot.Service;

import com.smartspend.copilot.client.GeminiClient;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.service.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AIServiceTest {
    // Arrange
    @Mock
    GeminiClient geminiClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AIService aiService;

    @BeforeEach
    void setUp(){
        aiService = new AIService(geminiClient, objectMapper);
    }

    @Test
    void shouldParseTransactionSuccessfully(){
        // Arrange
        String description = "Spend 15$ on pizza at Dominos";
        String fakeResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"amount\\":15.0,\\"category\\":\\"pizza\\",\\"merchant\\":\\"Dominos\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        when(geminiClient.generateContent(anyString())).thenReturn(fakeResponse);

        // Act
        Transaction result = aiService.parseTransaction(description);

        // Assert
        assertNotNull(result);
        assertEquals(15.0, result.getAmount());
        assertEquals("pizza", result.getCategory());
        assertEquals("Dominos", result.getMerchant());
    }

    @Test
    void shouldThrowExceptionWhenJsonParsingFail(){
        // Arrange
        String invalidResponse = "INVALID JSON RESPONSE";
        when(geminiClient.generateContent(anyString())).thenReturn(invalidResponse);

        // Act
        RuntimeException runtimeException = assertThrows(
                RuntimeException.class, // 预期抛出 RuntimeException
                ()-> aiService.parseTransaction("spent 15 dollars")// lambda: 把这段代码交给 JUnit 执行
        );

        // Assert
        assertEquals("Failed to parse AI response into Transaction object", runtimeException.getMessage());
        // 确认 geminiClient 的 generateContent() 真的被调用过
        // time(1): 必须刚好调用一次,只调用一次，避免多次要还bill啊，我没钱
        verify(geminiClient, times(1)).generateContent(anyString());
    }
}



// 【第一步：选好商家】
//    // 人话：你打开了外卖App，决定今天吃“麦当劳”（选好了POST请求）。
//    // 此时手机界面停留在：让你“输入外卖送货地址（URI）”。
//    @Mock
//    private RestClient.RequestBodyUriSpec requestBodyUriSpec; // restClient.post()
//
//    // 【第二步：选好地址】
//    // 人话：你把地址填好了（比如：第一郡）。
//    // 此时手机界面跳到了：让你“把汉堡炸鸡放进购物车（Body 请求体）”。
//    @Mock
//    private RestClient.RequestBodySpec requestBodySpec; // .uri("...").contentType()
//
//    // 【第三步：选好食物，准备付款】
//    // 人话：你把汉堡、可乐都加进购物车了。
//    // 此时手机界面停留在最后的【提交订单】按钮。你还可以顺便备注一下（加个Header）：比如“辣一点”、“不要香菜”。
//    @SuppressWarnings("rawtypes") //我知道这里有 generic warning ，不要烦我
//    @Mock
//    private RestClient.RequestHeadersSpec requestHeadersSpec; // .body
//
//    // 【第四步：外卖送到，等拆箱】
//    // 人话：你按了“提交订单”并付了钱。骑手把外卖送到你家门口了！
//    // 此时你手里提着这个“外卖打包盒（Response）”，正准备打开它（.body(String.class)）开始吃。
//    @Mock
//    private RestClient.ResponseSpec responseSpec; // .retrieve()
