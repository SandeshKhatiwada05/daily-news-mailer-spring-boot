package com.personal_news_prortal.newsportal;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiService {

    // Increased timeouts to 90s because Gemini 3 takes a second to "think"
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    public File generatePdf(String apiKey) throws Exception {

        JSONObject part = new JSONObject().put("text", Prompt.DAILY_NEWS);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject payload = new JSONObject().put("contents", new JSONArray().put(content));

        String json = payload.toString();
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        // MODEL UPDATE: gemini-3-flash-preview is the current 2026 free-tier workhorse
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        String rawJson = responseBody.string();
        System.out.println("GEMINI RESPONSE: " + rawJson);

        JSONObject root = new JSONObject(rawJson);

        if (!root.has("candidates")) {
            // This will help us see if it's a safety block or a quota issue
            throw new RuntimeException("Gemini error: " + rawJson);
        }

        JSONArray candidates = root.getJSONArray("candidates");
        String newsText = candidates
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        File pdf = new File("daily-news.pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdf));
        document.open();
        document.add(new Paragraph(newsText));
        document.close();

        return pdf;
    }
}