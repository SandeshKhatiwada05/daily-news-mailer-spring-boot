package com.personal_news_prortal.newsportal;

import com.lowagie.text.*;
import com.lowagie.text.Font;
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

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // Gemini 3 needs time for long dossiers
            .build();

    public File generatePdf(String apiKey) throws Exception {

        JSONObject part = new JSONObject().put("text", Prompt.DAILY_NEWS);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));

        // CRITICAL: Explicitly ask for a large output so your 30 stories don't get truncated
        JSONObject generationConfig = new JSONObject()
                .put("maxOutputTokens", 12000)
                .put("temperature", 0.7);

        JSONObject payload = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String rawJson = response.body().string();
        JSONObject root = new JSONObject(rawJson);

        if (!root.has("candidates")) throw new RuntimeException("Gemini error: " + rawJson);

        String newsText = root.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

        // --- BEAUTIFUL PDF GENERATION ---
        File pdf = new File("daily-news.pdf");
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(pdf));
        document.open();

        // Fonts for that "Premium Briefing" feel
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD);
        Font bodyFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.NORMAL);

        // Simple Markdown Parser Loop
        String[] lines = newsText.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                Paragraph p = new Paragraph(line.substring(2), titleFont);
                p.setSpacingBefore(20);
                p.setSpacingAfter(10);
                document.add(p);
            } else if (line.startsWith("## ")) {
                Paragraph p = new Paragraph(line.substring(3), sectionFont);
                p.setSpacingBefore(15);
                document.add(p);
            } else if (line.startsWith("* ") || line.startsWith("- ")) {
                ListItem item = new ListItem(line.substring(2), bodyFont);
                item.setIndentationLeft(20);
                document.add(item);
            } else if (!line.trim().isEmpty()) {
                Paragraph p = new Paragraph(line, bodyFont);
                p.setAlignment(Element.ALIGN_JUSTIFIED);
                document.add(p);
            }
        }

        document.close();
        return pdf;
    }
}