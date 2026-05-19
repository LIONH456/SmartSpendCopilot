package com.smartspend.copilot.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClient {
    @Value("${gemini.api.key}")
    private String apiKey;
    private final RestClient restClient;

    public GeminiClient(RestClient restClient){
        this.restClient = restClient;
    }

    public String generateContent(String requestBody){
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

}
