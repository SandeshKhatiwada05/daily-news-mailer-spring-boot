package com.personal_news_prortal.newsportal;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builds a beautiful, professional-grade PDF from structured JSON news data.
 * No markdown parsing. Full layout control.
 */
public class PdfBuilder {

    // ── Color Palette ────────────────────────────────────────────────────────
    private static final Color COLOR_BG_DARK       = new Color(15, 20, 35);      // near-black navy
    private static final Color COLOR_ACCENT_GOLD   = new Color(212, 175, 55);    // gold
    private static final Color COLOR_ACCENT_BLUE   = new Color(64, 156, 255);    // electric blue
    private static final Color COLOR_ACCENT_RED    = new Color(220, 60, 60);     // alert red
    private static final Color COLOR_ACCENT_GREEN  = new Color(50, 200, 120);    // verified green
    private static final Color COLOR_TEXT_PRIMARY  = new Color(240, 240, 245);   // near-white
    private static final Color COLOR_TEXT_SECONDARY = new Color(170, 175, 195);  // muted
    private static final Color COLOR_CARD_BG       = new Color(28, 35, 55);      // card dark
    private static final Color COLOR_SEPARATOR     = new Color(45, 55, 80);      // subtle line

    private static final Color[] SECTION_COLORS = {
            new Color(64, 156, 255),   // global — blue
            new Color(255, 140, 50),   // nepal  — orange
            new Color(120, 200, 80)    // tech   — green
    };

    // ── Fonts ────────────────────────────────────────────────────────────────
    private static Font font(String base, int size, int style, Color color) {
        return FontFactory.getFont(base, size, style, color);
    }

    public static File build(JSONObject data) throws Exception {
        File pdf = new File("daily-news.pdf");
        Document doc = new Document(PageSize.A4, 45, 45, 50, 60);

        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(pdf));
        writer.setPageEvent(new PageDecorator()); // footer + page numbers

        doc.open();

        String date = data.optString("date",
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        // ── Cover Page ───────────────────────────────────────────────────────
        addCoverPage(doc, writer, date);
        doc.newPage();

        // ── Table of Contents ────────────────────────────────────────────────
        addTableOfContents(doc, writer);
        doc.newPage();

        // ── Sections ─────────────────────────────────────────────────────────
        JSONArray sections = data.getJSONArray("sections");
        for (int s = 0; s < sections.length(); s++) {
            JSONObject section = sections.getJSONObject(s);
            Color accent = SECTION_COLORS[Math.min(s, SECTION_COLORS.length - 1)];
            addSection(doc, writer, section, accent, s + 1);
        }

        // ── Biggest Stories ───────────────────────────────────────────────────
        if (data.has("biggest_stories")) {
            doc.newPage();
            addBiggestStories(doc, writer, data.getJSONObject("biggest_stories"));
        }

        doc.close();
        return pdf;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // COVER PAGE
    // ═════════════════════════════════════════════════════════════════════════
    private static void addCoverPage(Document doc, PdfWriter writer, String date) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float w = doc.getPageSize().getWidth();
        float h = doc.getPageSize().getHeight();

        // Full background
        cb.setColorFill(COLOR_BG_DARK);
        cb.rectangle(0, 0, w, h);
        cb.fill();

        // Gold top bar
        cb.setColorFill(COLOR_ACCENT_GOLD);
        cb.rectangle(0, h - 8, w, 8);
        cb.fill();

        // Bottom bar
        cb.setColorFill(COLOR_ACCENT_GOLD);
        cb.rectangle(0, 0, w, 5);
        cb.fill();

        // Left accent stripe
        cb.setColorFill(new Color(COLOR_ACCENT_GOLD.getRed(),
                COLOR_ACCENT_GOLD.getGreen(),
                COLOR_ACCENT_GOLD.getBlue(), 60));
        cb.rectangle(0, 0, 6, h);
        cb.fill();

        // Center box
        float bx = 60, by = h * 0.3f, bw = w - 120, bh = h * 0.4f;
        cb.setColorFill(COLOR_CARD_BG);
        roundRect(cb, bx, by, bw, bh, 12);
        cb.fill();

        // Gold line inside box
        cb.setColorFill(COLOR_ACCENT_GOLD);
        cb.rectangle(bx + 30, by + bh - 4, bw - 60, 3);
        cb.fill();

        // Title text — use ColumnText for precise placement
        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(bx + 20, by, bx + bw - 20, by + bh - 10);
        ct.setAlignment(Element.ALIGN_CENTER);

        ct.addElement(spacer(40));
        ct.addElement(para("DAILY INTELLIGENCE REPORT",
                font(FontFactory.HELVETICA_BOLD, 26, Font.BOLD, COLOR_ACCENT_GOLD),
                Element.ALIGN_CENTER));
        ct.addElement(spacer(8));
        ct.addElement(para("Your curated briefing — 30 stories across 3 domains",
                font(FontFactory.HELVETICA, 11, Font.ITALIC, COLOR_TEXT_SECONDARY),
                Element.ALIGN_CENTER));
        ct.addElement(spacer(20));
        ct.addElement(hRule(cb, (int)(bw - 60)));
        ct.addElement(spacer(20));
        ct.addElement(para(date.toUpperCase(),
                font(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, COLOR_TEXT_PRIMARY),
                Element.ALIGN_CENTER));
        ct.addElement(spacer(16));

        // Section badges
        Phrase badges = new Phrase();
        addBadge(badges, "  GLOBAL  ", SECTION_COLORS[0]);
        addBadge(badges, "  NEPAL   ", SECTION_COLORS[1]);
        addBadge(badges, "  TECH    ", SECTION_COLORS[2]);
        Paragraph badgePara = new Paragraph(badges);
        badgePara.setAlignment(Element.ALIGN_CENTER);
        ct.addElement(badgePara);

        ct.go();

        // Bottom tagline
        ColumnText bottom = new ColumnText(cb);
        bottom.setSimpleColumn(0, 20, w, 55);
        bottom.addElement(para("Powered by Gemini AI  ·  Generated automatically",
                font(FontFactory.HELVETICA, 9, Font.ITALIC, COLOR_TEXT_SECONDARY),
                Element.ALIGN_CENTER));
        bottom.go();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TABLE OF CONTENTS (simple)
    // ═════════════════════════════════════════════════════════════════════════
    private static void addTableOfContents(Document doc, PdfWriter writer) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float w = doc.getPageSize().getWidth();

        addPageHeader(cb, "TABLE OF CONTENTS", COLOR_ACCENT_GOLD, w);

        doc.add(spacer(50));

        String[][] entries = {
                {"1", "Global News", "10 stories — geopolitics, economy, diplomacy"},
                {"2", "Nepal News", "10 stories — politics, infrastructure, policy"},
                {"3", "Technology & Coding", "10 stories — AI, dev tools, cybersecurity"},
                {"4", "Today's Biggest Stories", "Top story per section"},
        };

        Color[] entryColors = {SECTION_COLORS[0], SECTION_COLORS[1], SECTION_COLORS[2], COLOR_ACCENT_GOLD};

        for (int i = 0; i < entries.length; i++) {
            addTocEntry(doc, entries[i][0], entries[i][1], entries[i][2], entryColors[i]);
        }
    }

    private static void addTocEntry(Document doc, String num, String title, String sub, Color color) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{0.08f, 0.92f});
        table.setWidthPercentage(90);
        table.setSpacingBefore(12);

        // Number badge cell
        PdfPCell numCell = new PdfPCell();
        numCell.setBackgroundColor(color);
        numCell.setBorder(Rectangle.NO_BORDER);
        numCell.setPadding(8);
        Paragraph np = new Paragraph(num, font(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, Color.WHITE));
        np.setAlignment(Element.ALIGN_CENTER);
        numCell.addElement(np);
        table.addCell(numCell);

        // Title cell
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(COLOR_CARD_BG);
        titleCell.setBorder(PdfPCell.NO_BORDER);
        titleCell.enableBorderSide(PdfPCell.LEFT);
        titleCell.setBorderColorLeft(color);
        titleCell.setBorderWidthLeft(3);
        titleCell.setPadding(8);
        titleCell.addElement(new Paragraph(title,
                font(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, COLOR_TEXT_PRIMARY)));
        titleCell.addElement(new Paragraph(sub,
                font(FontFactory.HELVETICA, 10, Font.ITALIC, COLOR_TEXT_SECONDARY)));
        table.addCell(titleCell);

        doc.add(table);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION
    // ═════════════════════════════════════════════════════════════════════════
    private static void addSection(Document doc, PdfWriter writer,
                                   JSONObject section, Color accent, int sectionNum) throws Exception {
        doc.newPage();
        PdfContentByte cb = writer.getDirectContent();
        float w = doc.getPageSize().getWidth();

        String title = section.optString("title", "Section " + sectionNum);
        addPageHeader(cb, "SECTION " + sectionNum + " — " + title, accent, w);
        doc.add(spacer(50));

        JSONArray stories = section.getJSONArray("stories");
        for (int i = 0; i < stories.length(); i++) {
            addStoryCard(doc, stories.getJSONObject(i), i + 1, accent);
            if (i < stories.length() - 1) {
                doc.add(spacer(6));
            }
        }
    }

    private static void addStoryCard(Document doc, JSONObject story, int num, Color accent) throws Exception {

        // ── Story number + headline ──────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(new float[]{0.07f, 0.93f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(14);

        PdfPCell numCell = new PdfPCell(new Paragraph(String.valueOf(num),
                font(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, Color.WHITE)));
        numCell.setBackgroundColor(accent);
        numCell.setBorder(Rectangle.NO_BORDER);
        numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        numCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        numCell.setPadding(6);
        headerTable.addCell(numCell);

        PdfPCell headlineCell = new PdfPCell(new Paragraph(
                story.optString("headline", ""),
                font(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, COLOR_TEXT_PRIMARY)));
        headlineCell.setBackgroundColor(COLOR_CARD_BG);
        headlineCell.setBorder(PdfPCell.NO_BORDER);
        headlineCell.enableBorderSide(PdfPCell.BOTTOM);
        headlineCell.setBorderColorBottom(accent);
        headlineCell.setBorderWidthBottom(2f);
        headlineCell.setPadding(8);
        headerTable.addCell(headlineCell);

        doc.add(headerTable);

        // ── Card body ────────────────────────────────────────────────────────
        PdfPTable body = new PdfPTable(1);
        body.setWidthPercentage(100);

        PdfPCell bodyCell = new PdfPCell();
        bodyCell.setBackgroundColor(new Color(22, 28, 45));
        bodyCell.setBorder(PdfPCell.NO_BORDER);
        bodyCell.enableBorderSide(PdfPCell.LEFT);
        bodyCell.enableBorderSide(PdfPCell.RIGHT);
        bodyCell.enableBorderSide(PdfPCell.BOTTOM);
        bodyCell.setBorderColorLeft(COLOR_SEPARATOR);
        bodyCell.setBorderColorRight(COLOR_SEPARATOR);
        bodyCell.setBorderColorBottom(COLOR_SEPARATOR);
        bodyCell.setBorderWidth(1f);
        bodyCell.setPadding(12);

        // What Happened
        bodyCell.addElement(labeledSection("WHAT HAPPENED", accent));
        bodyCell.addElement(new Paragraph(story.optString("what_happened", ""),
                font(FontFactory.TIMES_ROMAN, 10, Font.NORMAL, COLOR_TEXT_PRIMARY)));

        // Plain English
        bodyCell.addElement(spacer(8));
        bodyCell.addElement(labeledSection("PLAIN ENGLISH", new Color(100, 220, 180)));
        Paragraph plainPara = new Paragraph(story.optString("plain_english", ""),
                font(FontFactory.HELVETICA, 10, Font.ITALIC, new Color(200, 240, 220)));
        plainPara.setIndentationLeft(10);
        bodyCell.addElement(plainPara);

        // Sarcasm
        bodyCell.addElement(spacer(8));
        bodyCell.addElement(labeledSection("REALITY CHECK", new Color(255, 140, 90)));
        Paragraph sarcasmPara = new Paragraph(story.optString("sarcasm", ""),
                font(FontFactory.HELVETICA, 10, Font.ITALIC, new Color(255, 200, 150)));
        sarcasmPara.setIndentationLeft(10);
        bodyCell.addElement(sarcasmPara);

        // Sources
        bodyCell.addElement(spacer(6));
        Paragraph srcPara = new Paragraph("SOURCES: " + story.optString("sources", ""),
                font(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_TEXT_SECONDARY));
        bodyCell.addElement(srcPara);

        body.addCell(bodyCell);
        doc.add(body);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BIGGEST STORIES
    // ═════════════════════════════════════════════════════════════════════════
    private static void addBiggestStories(Document doc, PdfWriter writer, JSONObject biggest) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float w = doc.getPageSize().getWidth();

        addPageHeader(cb, "TODAY'S BIGGEST STORIES", COLOR_ACCENT_GOLD, w);
        doc.add(spacer(50));

        String[][] keys = {
                {"global", "GLOBAL", "SECTION_COLORS[0]"},
                {"nepal",  "NEPAL",  ""},
                {"tech",   "TECH",   ""}
        };
        Color[] bColors = {SECTION_COLORS[0], SECTION_COLORS[1], SECTION_COLORS[2]};
        String[] labels = {"GLOBAL", "NEPAL", "TECH"};

        String[] jsonKeys = {"global", "nepal", "tech"};
        for (int i = 0; i < jsonKeys.length; i++) {
            if (!biggest.has(jsonKeys[i])) continue;
            JSONObject s = biggest.getJSONObject(jsonKeys[i]);
            addBigCard(doc, labels[i], s.optString("headline"), s.optString("summary"), bColors[i]);
        }
    }

    private static void addBigCard(Document doc, String label, String headline, String summary, Color color) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_CARD_BG);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.enableBorderSide(PdfPCell.LEFT);
        cell.setBorderColorLeft(color);
        cell.setBorderWidthLeft(5f);
        cell.setPadding(14);

        Paragraph labelPara = new Paragraph("★  " + label + " TOP STORY",
                font(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, color));
        cell.addElement(labelPara);
        cell.addElement(spacer(4));
        cell.addElement(new Paragraph(headline,
                font(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, COLOR_TEXT_PRIMARY)));
        cell.addElement(spacer(6));
        cell.addElement(new Paragraph(summary,
                font(FontFactory.TIMES_ROMAN, 11, Font.NORMAL, COLOR_TEXT_SECONDARY)));

        table.addCell(cell);
        doc.add(table);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PAGE DECORATOR (footer / page numbers)
    // ═════════════════════════════════════════════════════════════════════════
    static class PageDecorator extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            float w = doc.getPageSize().getWidth();
            float h = doc.getPageSize().getHeight();

            // Footer line
            cb.setColorStroke(COLOR_SEPARATOR);
            cb.setLineWidth(0.5f);
            cb.moveTo(40, 45);
            cb.lineTo(w - 40, 45);
            cb.stroke();

            // Page number
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.valueOf(writer.getPageNumber()),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_TEXT_SECONDARY)),
                    w / 2, 30, 0);

            // Footer text
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Daily Intelligence Report",
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, COLOR_TEXT_SECONDARY)),
                    40, 30, 0);

            // Right footer
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Confidential · Personal Use",
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, COLOR_TEXT_SECONDARY)),
                    w - 40, 30, 0);

            // Gold bottom bar
            cb.setColorFill(COLOR_ACCENT_GOLD);
            cb.rectangle(0, 0, w, 3);
            cb.fill();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private static void addPageHeader(PdfContentByte cb, String text, Color color, float pageWidth) {
        // Dark banner
        cb.setColorFill(color);
        cb.rectangle(0, PageSize.A4.getHeight() - 50, pageWidth, 50);
        cb.fill();

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, Color.WHITE)),
                pageWidth / 2, PageSize.A4.getHeight() - 30, 0);
    }

    private static Paragraph labeledSection(String label, Color color) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, color);
        Paragraph p = new Paragraph("▶ " + label, f);
        p.setSpacingBefore(4);
        return p;
    }

    private static Paragraph para(String text, Font font, int align) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(align);
        return p;
    }

    private static Element spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(height);
        return p;
    }

    private static Element hRule(PdfContentByte cb, int width) {
        // Return empty paragraph — visual line drawn separately via canvas
        Paragraph p = new Paragraph(" ");
        p.setLeading(1);
        return p;
    }

    private static void addBadge(Phrase parent, String text, Color bg) {
        // Simple colored text badge (OpenPDF doesn't support Chunk background natively in all versions)
        Chunk chunk = new Chunk(" " + text + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, bg));
        parent.add(chunk);
        parent.add(new Chunk("  "));
    }

    private static void roundRect(PdfContentByte cb, float x, float y, float w, float h, float r) {
        cb.roundRectangle(x, y, w, h, r);
    }
}