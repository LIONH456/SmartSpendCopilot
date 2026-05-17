package com.smartspend.copilot.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class ExchangeRateService {
    private static final String PROVIDER_HOST = "api.exchangerate.host";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final double DEFAULT_USD_TO_VND = 25000.0;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile double cachedRate = DEFAULT_USD_TO_VND;
    private volatile Instant cacheTimestamp = Instant.EPOCH;

    public double getRate(String base, String target) {
        if (base.equalsIgnoreCase(target)) {
            return 1.0;
        }

        if (base.equalsIgnoreCase("USD") && target.equalsIgnoreCase("VND")) {
            if (isCacheValid()) {
                return cachedRate;
            }

            synchronized (this) {
                if (isCacheValid()) {
                    return cachedRate;
                }
                double rate = fetchRateFromProvider(base, target);
                cachedRate = rate;
                cacheTimestamp = Instant.now();
                return rate;
            }
        }

        if (base.equalsIgnoreCase("VND") && target.equalsIgnoreCase("USD")) {
            return 1.0 / getRate("USD", "VND");
        }

        throw new IllegalArgumentException("Unsupported currency pair: " + base + " -> " + target);
    }

    private boolean isCacheValid() {
        return cacheTimestamp.plus(CACHE_TTL).isAfter(Instant.now());
    }

    private double fetchRateFromProvider(String base, String target) {
        try {
            String url = String.format("https://%s/latest?base=%s&symbols=%s", PROVIDER_HOST, base.toUpperCase(Locale.ROOT), target.toUpperCase(Locale.ROOT));
            String response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode rates = rootNode.path("rates");
            if (rates.has(target.toUpperCase(Locale.ROOT))) {
                return rates.get(target.toUpperCase(Locale.ROOT)).asDouble();
            }
        } catch (Exception ignored) {
            // Fallback to default rate if provider is unavailable or returns invalid data.
        }

        return getDefaultRate(base, target);
    }

    private double getDefaultRate(String base, String target) {
        if (base.equalsIgnoreCase("USD") && target.equalsIgnoreCase("VND")) {
            return DEFAULT_USD_TO_VND;
        }
        if (base.equalsIgnoreCase("VND") && target.equalsIgnoreCase("USD")) {
            return 1.0 / DEFAULT_USD_TO_VND;
        }
        return 1.0;
    }
}
