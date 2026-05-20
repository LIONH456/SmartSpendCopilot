package com.smartspend.copilot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Slf4j
@Component
public class ExchangeRateClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${exchange.api.base-url}")
    private String baseUrl;

    public ExchangeRateClient(
            RestClient restClient,
            ObjectMapper objectMapper
    ) {

        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public double fetchRate(String base, String target) {

        try {

            String url = String.format(
                    baseUrl,
                    base.toUpperCase(Locale.ROOT)
            );

            String response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(response);

            JsonNode rates = rootNode.path("rates");

            if (rates.has(target.toUpperCase(Locale.ROOT))) {

                return rates
                        .get(target.toUpperCase(Locale.ROOT))
                        .asDouble();
            }

            throw new RuntimeException(
                    "Currency not found in API response"
            );

        } catch (Exception e) {

            log.error(
                    "Failed to fetch exchange rate: {} -> {}",
                    base,
                    target,
                    e
            );

            throw new RuntimeException(
                    "Failed to fetch exchange rate"
            );
        }
    }
}