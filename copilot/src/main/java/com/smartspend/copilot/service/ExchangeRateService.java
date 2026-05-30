package com.smartspend.copilot.service;

import com.smartspend.copilot.client.ExchangeRateClient;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class ExchangeRateService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(1440);
    private static final double DEFAULT_USD_TO_VND = 25000;
    private final ExchangeRateClient exchangeRateClient;
    private volatile double cachedRate = DEFAULT_USD_TO_VND;
    private volatile Instant cacheTimeStamp = Instant.EPOCH;

    public ExchangeRateService(
            ExchangeRateClient exchangeRateClient
    ) {
        this.exchangeRateClient = exchangeRateClient;
    }

    public double getRate(String base, String target) {
        validateSupportedPair(base, target);
        if (base.equalsIgnoreCase(target)) {
            return 1.0;
        }

        if (base.equalsIgnoreCase("USD")
                && target.equalsIgnoreCase("VND")) {

            return getUsdToVndRate();
        }

        return 1.0 / getUsdToVndRate();
    }

    private double getUsdToVndRate() {

        if (isCacheValid()) {
            return cachedRate;
        }

        synchronized (this) {

            if (isCacheValid()) {
                return cachedRate;
            }
            try {
                double rate = exchangeRateClient.fetchRate("USD", "VND");
                cachedRate = rate;
                cacheTimeStamp = Instant.now();
                return rate;
            } catch (Exception e) {
                log.error("Using fallback exchange rate", e);
                return DEFAULT_USD_TO_VND;
            }
        }
    }

    private void validateSupportedPair(String base, String target) {

        boolean supported =
                (base.equalsIgnoreCase("USD")
                        && target.equalsIgnoreCase("VND")) ||
                        (base.equalsIgnoreCase("VND")
                                && target.equalsIgnoreCase("USD")) ||
                        base.equalsIgnoreCase(target);

        if (!supported) {

            throw new AppException(
                    ErrorCode.UNSUPPORTED_CURRENCY_PAIR,
                    "Unsupported currency pair: " + base + " -> " + target
            );
        }
    }


    private boolean isCacheValid() {
        return cacheTimeStamp.plus(CACHE_TTL).isAfter(Instant.now());
    }
}