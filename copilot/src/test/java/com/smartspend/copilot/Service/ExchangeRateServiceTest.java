package com.smartspend.copilot.Service;


import com.smartspend.copilot.client.ExchangeRateClient;
import com.smartspend.copilot.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {
    // Arrange
    @Mock
    ExchangeRateClient exchangeRateClient;

    // ObjectMapper objectMapper = new ObjectMapper();

    // @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp(){
        exchangeRateService = new ExchangeRateService(exchangeRateClient);
    }

    @Test
    void shouldReturnOneWhenCurrenciesAreSame(){
        // Arrange
        String base = "USD";
        String target = "USD";

        // Act
        double result = exchangeRateService.getRate(base,target);

        // Assert
        assertEquals(1.0, result);
    }

    @Test
    void shouldReturnFetchedUsdToVndRate(){
        // arrange
        when(exchangeRateClient.fetchRate("USD", "VND")).thenReturn(26000.0);

        // act
        double result = exchangeRateService.getRate("USD", "VND");


        // assert
        assertEquals(26000.0, result);
        // 确认这个 method 被调用过
        verify(exchangeRateClient).fetchRate("USD", "VND");
    }

    @Test
    void shouldUseCachedRateOnSecondCall(){
        // Arrange
        when(exchangeRateClient.fetchRate("USD", "VND")).thenReturn(26000.0);

        // act
        double firstResult = exchangeRateService.getRate("USD", "VND");
        double secondResult = exchangeRateService.getRate("USD", "VND");

        // assert
        assertEquals(26000.0, firstResult);
        assertEquals(26000.0, secondResult);

        // make sure cache are being called once
        verify(exchangeRateClient, times(1)).fetchRate("USD", "VND");
    }

    @Test
    void shouldThrowExceptionForUnsupportedCurrencyPair(){
        // Arrange
        String base = "EUR";
        String target = "GBP";

        // Act
        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> exchangeRateService.getRate(base, target)
        );

        // Assert
        assertEquals("Unsupported currency pair: "+ base + " -> " + target,
                illegalArgumentException.getMessage());
    }

    @Test
    void shouldReturnDefaultRateWhenClientFails(){
        // Arrange
        when(exchangeRateClient.fetchRate("USD", "VND")).thenThrow(IllegalArgumentException.class);

        // Act
        double result = exchangeRateService.getRate("USD", "VND");

        // Assert
        assertEquals(25000.0, result);
        verify(exchangeRateClient).fetchRate("USD", "VND");
    }


}
