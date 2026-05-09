package com.personal_news_prortal.newsportal;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    public File generatePdf(String apiKey) throws Exception {

        // ── 1. Ask Gemini for structured JSON ─────────────────────────────────
        JSONObject part = new JSONObject().put("text", Prompt.DAILY_NEWS_JSON);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));

        JSONObject generationConfig = new JSONObject()
                .put("maxOutputTokens", 16000)   // need room for 30 stories
                .put("temperature", 0.65)
                .put("responseMimeType", "application/json"); // tell Gemini: JSON only

        JSONObject payload = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .post(body)
                .build();

        // ── 2. Parse response ────────────────────────────────────────────────
        Response response = client.newCall(request).execute();
        String rawJson = response.body().string();

        JSONObject root = new JSONObject(rawJson);
        if (!root.has("candidates")) {
            throw new RuntimeException("Gemini error: " + rawJson);
        }

        String newsJsonText = root
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Strip accidental markdown fences if Gemini adds them despite instructions
        newsJsonText = newsJsonText.trim();
        if (newsJsonText.startsWith("```")) {
            newsJsonText = newsJsonText
                    .replaceAll("^```[a-zA-Z]*\\n?", "")
                    .replaceAll("```$", "")
                    .trim();
        }

        JSONObject newsData = new JSONObject(newsJsonText);

        // ── 3. Build beautiful PDF ───────────────────────────────────────────
        return PdfBuilder.build(newsData);
    }
}