package com.smartspend.copilot.service;

import com.smartspend.copilot.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AIService {
    @Value("${gemini.api.key}")
    private String apiKey;

    // Initializes Spring's modern HTTP client used to send POST requests to Google's servers.
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Transaction parseTransaction(String description){
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        // Structured prompt to guarantee strict JSON output matching our schema
        String requestBody = """
            {
              "contents": [{
                "parts":[{"text": "%s"}]
              }],
              "systemInstruction": {
                "parts": [{"text": "You are an expense parser. Extract the transaction into exactly this JSON schema: {\\"amount\\": number, \\"category\\": string, \\"merchant\\": string}. Return ONLY valid JSON."}]
              },
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
            """.formatted(description.replace("\"", "\\\""));

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            // Traverse the Gemini JSON response wrapper to get the inner text block
            JsonNode rootNode = objectMapper.readTree(response);
            String aiGeneratedJson = rootNode
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();
            return objectMapper.readValue(aiGeneratedJson, Transaction.class);
        }catch (Exception e){
            throw new RuntimeException("Failed to parse AI response into Transaction object", e);
        }
    }
}
