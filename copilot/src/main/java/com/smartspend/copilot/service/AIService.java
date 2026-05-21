package com.smartspend.copilot.service;

import com.smartspend.copilot.client.GeminiClient;
import com.smartspend.copilot.entity.Transaction;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AIService {

    private GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public AIService(GeminiClient geminiClient, ObjectMapper objectMapper){
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public Transaction parseTransaction(String description){
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

        String response = geminiClient.generateContent(requestBody);

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
