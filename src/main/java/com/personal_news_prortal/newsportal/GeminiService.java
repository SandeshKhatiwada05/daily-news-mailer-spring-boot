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
import java.util.concurrent.TimeUnit; // Import this!

@Service
public class GeminiService {

    // 1. Create a client with long timeouts (60-90 seconds)
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

        // 2. Using 'gemini-1.5-flash' which is the stable target for v1beta right now
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
                .post(body)
                .build();

        // 3. The execution now uses the custom 'client' defined above
        Response response = client.newCall(request).execute();

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        String rawJson = responseBody.string();

        System.out.println("GEMINI RESPONSE: " + rawJson);

        JSONObject root = new JSONObject(rawJson);

        if (!root.has("candidates")) {
            throw new RuntimeException("Gemini error: " + rawJson);
        }

        JSONArray candidates = root.getJSONArray("candidates");
        String newsText = candidates
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Write to PDF
        File pdf = new File("daily-news.pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdf));
        document.open();
        document.add(new Paragraph(newsText));
        document.close();

        return pdf;
    }
}