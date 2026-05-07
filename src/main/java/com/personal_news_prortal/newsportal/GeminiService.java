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

@Service
public class GeminiService {

    public File generatePdf(String apiKey) throws Exception {

        OkHttpClient client = new OkHttpClient();
        JSONObject part = new JSONObject().put("text", Prompt.DAILY_NEWS);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject payload = new JSONObject().put("contents", new JSONArray().put(content));

        String json = payload.toString();
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        String rawJson = responseBody.string();

        System.out.println("GEMINI RESPONSE: " + rawJson);

        // Parse Gemini response to extract generated text
        JSONObject root = new JSONObject(rawJson);

        //check for errors
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

        // Write extracted text into a proper PDF using OpenPDF
        File pdf = new File("daily-news.pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdf));
        document.open();
        document.add(new Paragraph(newsText));
        document.close();

        return pdf;
    }
}