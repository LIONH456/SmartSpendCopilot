package com.smartspend.copilot.service;

import com.smartspend.copilot.client.ExchangeRateClient;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@Slf4j
public class ExchangeRateService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(1440); // cache for 1 day
    private static final double DEFAULT_USD_TO_VND = 25000;
    private final ExchangeRateClient exchangeRateClient;
    private volatile double cachedRate = DEFAULT_USD_TO_VND;
    private volatile Instant cacheTimeStamp = Instant.EPOCH;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    public double getRate(String base, String target){
        if(base.equalsIgnoreCase(target)){
            return 1.0;
        }

        if(base.equalsIgnoreCase("USD") && target.equalsIgnoreCase("VND")){
            if(isCacheValid()){
                return cachedRate;
            }

            synchronized(this){
                if(isCacheValid()){
                    return cachedRate;
                }
                try{
                    double rate = exchangeRateClient.fetchRate(base, target);

                    if(rate > 0){
                        cachedRate = rate;
                        cacheTimeStamp = Instant.now();
                        return rate;
                    }
                }catch(Exception e){
                    log.error("Failed to fetch exchange rate");
                }

                return DEFAULT_USD_TO_VND; // 没办法，fetch不到就用defaultRate
            }
        }
        if(base.equalsIgnoreCase("VND") && target.equalsIgnoreCase("USD")){
            return 1.0/getRate("USD", "VND");
        }
        throw new IllegalArgumentException(
                "Unsupported currency pair: "
                + base
                + " -> "
                + target
        );
    }

    private boolean isCacheValid(){
        return cacheTimeStamp.plus(CACHE_TTL).isAfter(Instant.now());
    }
}
