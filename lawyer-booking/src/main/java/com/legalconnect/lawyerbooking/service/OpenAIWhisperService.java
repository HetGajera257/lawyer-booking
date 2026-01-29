package com.legalconnect.lawyerbooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpenAIWhisperService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String TRANSLATE_URL =
            "https://api.openai.com/v1/audio/translations";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ================= Gujarati Audio → English Text =================
    public String translateToEnglish(MultipartFile file) throws Exception {

        MediaType mediaType = MediaType.parse(
                file.getContentType() != null ? file.getContentType() : "audio/wav"
        );

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        file.getOriginalFilename(),
                        RequestBody.create(file.getBytes(), mediaType)
                )
                // 🔥 STABLE MODEL
                .addFormDataPart("model", "whisper-1")
                .build();

        Request request = new Request.Builder()
                .url(TRANSLATE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {

            String responseBody = response.body() != null
                    ? response.body().string()
                    : "";

            System.out.println("Whisper Translation Response: " + responseBody);

            if (!response.isSuccessful()) {
                throw new RuntimeException("OpenAI error: " + responseBody);
            }

            JsonNode json = mapper.readTree(responseBody);

            if (!json.has("text")) {
                throw new RuntimeException("Invalid Whisper response: " + responseBody);
            }

            return json.get("text").asText();
        }
    }
}
