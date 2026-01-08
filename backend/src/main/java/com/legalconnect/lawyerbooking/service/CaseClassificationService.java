package com.legalconnect.lawyerbooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CaseClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(CaseClassificationService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String[]> KEYWORD_MAP = new HashMap<>();

    static {
        KEYWORD_MAP.put("FAMILY_LAW", new String[]{"divorce", "custody", "alimony", "marriage", "child", "spouse", "parent"});
        KEYWORD_MAP.put("CRIMINAL", new String[]{"theft", "assault", "fraud", "arrest", "police", "fir", "jail", "crime", "murder"});
        KEYWORD_MAP.put("PROPERTY", new String[]{"land", "rent", "deed", "house", "tenant", "landlord", "eviction", "mortgage"});
        KEYWORD_MAP.put("CORPORATE", new String[]{"business", "contract", "merger", "startup", "company", "shares", "partnership"});
        KEYWORD_MAP.put("CIVIL", new String[]{"dispute", "lawsuit", "compensation", "defamation", "negligence", "contract"});
    }

    private static final String CLASSIFICATION_PROMPT = """
            You are a legal expert. Analyze the following masked legal case description and classify it into one of the following categories:
            - FAMILY_LAW
            - CRIMINAL
            - PROPERTY
            - CORPORATE
            - CIVIL
            - OTHER
            
            Provide ONLY the category name as the output.
            
            Case Description:
            """;

    public String classifyCase(String maskedText) {
        if (maskedText == null || maskedText.trim().isEmpty()) {
            return "OTHER";
        }

        // 1. Try AI Classification
        try {
            String aiResult = callOpenAI(maskedText);
            if (aiResult != null && !aiResult.equalsIgnoreCase("OTHER")) {
                return aiResult.toUpperCase().replace(" ", "_");
            }
        } catch (Exception e) {
            logger.warn("AI Classification failed, falling back to keywords: {}", e.getMessage());
        }

        // 2. Keyword Fallback
        return classifyWithKeywords(maskedText);
    }

    private String callOpenAI(String text) throws Exception {
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "gpt-4o-mini");
        
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a legal classification assistant.");
        messages.add(systemMessage);
        
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", CLASSIFICATION_PROMPT + text);
        messages.add(userMessage);
        
        requestJson.set("messages", messages);
        requestJson.put("temperature", 0.0);

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(requestJson),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            JsonNode json = mapper.readTree(response.body().string());
            return json.get("choices").get(0).get("message").get("content").asText().trim();
        }
    }

    private String classifyWithKeywords(String text) {
        String lowerText = text.toLowerCase();
        for (Map.Entry<String, String[]> entry : KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword)) {
                    logger.info("Classified via keyword '{}' as {}", keyword, entry.getKey());
                    return entry.getKey();
                }
            }
        }
        return "OTHER";
    }
}
