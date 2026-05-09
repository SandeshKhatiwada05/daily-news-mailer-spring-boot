package com.personal_news_prortal.newsportal;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builds a beautiful PDF matching the light cream/tan design from the reference image.
 *
 * Palette from reference:
 *   #E8DDB4  warm cream  — page background
 *   #767F9E  slate-blue  — cards / inset panels
 *   #DAA464  warm gold   — accents, date bar, section labels
 *   #DEC384  light tan   — secondary accent, PLAIN ENGLISH badge
 *
 * Layout: every story gets its own full page (one-story-per-page).
 */
public class PdfBuilder {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color C_PAGE_BG   = new Color(232, 221, 180);   // #E8DDB4
    private static final Color C_CARD      = new Color(118, 127, 158);   // #767F9E
    private static final Color C_GOLD      = new Color(218, 164, 100);   // #DAA464
    private static final Color C_TAN       = new Color(222, 195, 132);   // #DEC384
    private static final Color C_TEXT_DARK = new Color(38,  38,  38);    // body text on light bg
    private static final Color C_TEXT_MID  = new Color(65,  65,  65);    // secondary body
    private static final Color C_TEXT_LITE = new Color(245, 240, 232);   // text on dark card bg
    private static final Color C_MUTED     = new Color(105, 100,  90);   // sources / footer
    private static final Color C_RULE      = new Color(200, 188, 148);   // separator lines

    private static final Color[] SECTION_ACCENT = { C_GOLD, C_TAN, new Color(195, 170, 100) };

    // ── Font helper ───────────────────────────────────────────────────────────
    private static Font f(String base, float size, int style, Color color) {
        return FontFactory.getFont(base, size, style, color);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ═════════════════════════════════════════════════════════════════════════
    public static File build(JSONObject data) throws Exception {
        File pdf = new File("daily-news.pdf");
        Document doc = new Document(PageSize.A4, 50, 50, 55, 65);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(pdf));
        writer.setPageEvent(new FooterPainter());
        doc.open();

        String date = data.optString("date",
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        addCover(doc, writer, date);
        doc.newPage();

        addToc(doc, writer);
        doc.newPage();

        JSONArray sections = data.getJSONArray("sections");
        for (int s = 0; s < sections.length(); s++) {
            JSONObject section = sections.getJSONObject(s);
            Color accent = SECTION_ACCENT[Math.min(s, SECTION_ACCENT.length - 1)];
            String sectionTitle = section.optString("title", "News");
            JSONArray stories = section.getJSONArray("stories");
            for (int i = 0; i < stories.length(); i++) {
                addStoryPage(doc, writer, stories.getJSONObject(i),
                        i + 1, sectionTitle, accent, date);
                doc.newPage();
            }
        }

        if (data.has("biggest_stories")) {
            addBiggestPage(doc, writer, data.getJSONObject("biggest_stories"));
        }

        doc.close();
        return pdf;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // COVER
    // ═════════════════════════════════════════════════════════════════════════
    private static void addCover(Document doc, PdfWriter writer, String date) throws Exception {
        paintBg(writer);
        PdfContentByte cb = writer.getDirectContent();
        float w = PageSize.A4.getWidth();
        float h = PageSize.A4.getHeight();

        // Gold top + bottom strips
        cb.setColorFill(C_GOLD);
        cb.rectangle(0, h - 7, w, 7);
        cb.fill();
        cb.setColorFill(C_GOLD);
        cb.rectangle(0, 0, w, 7);
        cb.fill();

        // Central slate card
        float cx = 55, cy = h * 0.26f, cw = w - 110, ch = h * 0.48f;
        cb.setColorFill(C_CARD);
        cb.roundRectangle(cx, cy, cw, ch, 10);
        cb.fill();

        // Gold accent bar at top of card
        cb.setColorFill(C_GOLD);
        cb.roundRectangle(cx, cy + ch - 7, cw, 7, 4);
        cb.fill();

        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(cx + 28, cy + 10, cx + cw - 28, cy + ch - 20);

        ct.addElement(spacer(24));

        Paragraph t1 = new Paragraph("DAILY INTELLIGENCE REPORT",
                f(FontFactory.HELVETICA_BOLD, 21, Font.BOLD, C_GOLD));
        t1.setAlignment(Element.ALIGN_CENTER);
        ct.addElement(t1);

        ct.addElement(spacer(5));

        Paragraph t2 = new Paragraph("30 stories  ·  Global  ·  Nepal  ·  Technology",
                f(FontFactory.HELVETICA, 10, Font.ITALIC, C_TAN));
        t2.setAlignment(Element.ALIGN_CENTER);
        ct.addElement(t2);

        ct.addElement(spacer(18));
        ct.addElement(new Chunk(new LineSeparator(0.5f, 65, C_TAN, Element.ALIGN_CENTER, 0)));
        ct.addElement(spacer(18));

        Paragraph dp = new Paragraph(date.toUpperCase(),
                f(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, C_TEXT_LITE));
        dp.setAlignment(Element.ALIGN_CENTER);
        ct.addElement(dp);

        ct.addElement(spacer(14));

        Paragraph tag = new Paragraph("Curated  ·  Verified  ·  Delivered Daily",
                f(FontFactory.HELVETICA, 9, Font.ITALIC, C_TAN));
        tag.setAlignment(Element.ALIGN_CENTER);
        ct.addElement(tag);

        ct.go();

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Powered by Gemini AI  ·  Auto-generated",
                        f(FontFactory.HELVETICA, 7, Font.ITALIC, C_MUTED)),
                w / 2, 18, 0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TABLE OF CONTENTS
    // ═════════════════════════════════════════════════════════════════════════
    private static void addToc(Document doc, PdfWriter writer) throws Exception {
        paintBg(writer);
        paintTopBar(writer, "TABLE OF CONTENTS", C_GOLD);
        doc.add(spacer(44));

        String[][] rows = {
                {"Section 1", "Global News",          "10 stories — geopolitics, economy, wars, diplomacy"},
                {"Section 2", "Nepal News",            "10 stories — politics, infrastructure, public policy"},
                {"Section 3", "Technology & Coding",   "10 stories — AI, dev tools, security, open source"},
                {"Final",     "Today's Biggest Stories","Top headline per section, briefly explained"},
        };
        Color[] ec = { SECTION_ACCENT[0], SECTION_ACCENT[1], SECTION_ACCENT[2], C_GOLD };

        for (int i = 0; i < rows.length; i++) {
            PdfPTable t = new PdfPTable(new float[]{0.22f, 0.78f});
            t.setWidthPercentage(95);
            t.setSpacingBefore(10);

            PdfPCell lc = new PdfPCell();
            lc.setBackgroundColor(ec[i]);
            lc.setBorder(PdfPCell.NO_BORDER);
            lc.setPadding(10);
            Paragraph np = new Paragraph(rows[i][0],
                    f(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, C_TEXT_DARK));
            np.setAlignment(Element.ALIGN_CENTER);
            lc.addElement(np);
            t.addCell(lc);

            PdfPCell rc = new PdfPCell();
            rc.setBackgroundColor(C_CARD);
            rc.setBorder(PdfPCell.NO_BORDER);
            rc.setPadding(10);
            rc.addElement(new Paragraph(rows[i][1],
                    f(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, C_TEXT_LITE)));
            rc.addElement(new Paragraph(rows[i][2],
                    f(FontFactory.HELVETICA, 9, Font.ITALIC, C_TAN)));
            t.addCell(rc);

            doc.add(t);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STORY PAGE  — one full page per story, matching reference image
    // ═════════════════════════════════════════════════════════════════════════
    private static void addStoryPage(Document doc, PdfWriter writer,
                                     JSONObject story, int num,
                                     String sectionTitle, Color accent, String date) throws Exception {
        paintBg(writer);
        PdfContentByte cb = writer.getDirectContent();
        float w = PageSize.A4.getWidth();
        float h = PageSize.A4.getHeight();
        float lm = 50, rm = 50;

        // ── Meta line: DATE · SECTION · DAILY REPORT ─────────────────────────
        // Matches image top-left gold small-caps line
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(date.toUpperCase() + "  ·  " + sectionTitle.toUpperCase() + "  ·  DAILY REPORT",
                        f(FontFactory.HELVETICA_BOLD, 7, Font.BOLD, C_GOLD)),
                lm, h - 32, 0);

        // ── Headline ─────────────────────────────────────────────────────────
        // Placed just below meta line, large bold dark text
        ColumnText headCt = new ColumnText(cb);
        headCt.setSimpleColumn(lm, h - 115, w - rm, h - 42);
        Paragraph headline = new Paragraph(story.optString("headline", ""),
                f(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, C_TEXT_DARK));
        headline.setLeading(25);
        headCt.addElement(headline);
        headCt.go();

        // ── Horizontal rule under headline ────────────────────────────────────
        cb.setColorStroke(C_RULE);
        cb.setLineWidth(0.8f);
        cb.moveTo(lm, h - 120);
        cb.lineTo(w - rm, h - 120);
        cb.stroke();

        // ── Body content column ───────────────────────────────────────────────
        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(lm, 68, w - rm, h - 130);

        // ── 1. WHAT HAPPENED ─────────────────────────────────────────────────
        ct.addElement(smallBadge("1. WHAT HAPPENED", C_GOLD));
        ct.addElement(spacer(5));

        Paragraph whatPara = new Paragraph(story.optString("what_happened", ""),
                f(FontFactory.TIMES_ROMAN, 10.5f, Font.NORMAL, C_TEXT_MID));
        whatPara.setAlignment(Element.ALIGN_JUSTIFIED);
        whatPara.setLeading(15.5f);
        ct.addElement(whatPara);

        ct.addElement(spacer(14));

        // ── 2. ACTUALLY, WHAT THIS MEANS  (inset card — matches image's blue panel) ──
        ct.addElement(buildPanel(
                "2. ACTUALLY, WHAT THIS MEANS",
                story.optString("plain_english", ""),
                C_CARD, C_TAN, C_TEXT_LITE));

        ct.addElement(spacer(14));

        // ── 3. REALITY CHECK  (sarcasm, gold left-border quote) ──────────────
        ct.addElement(smallBadge("3. REALITY CHECK", C_TAN));
        ct.addElement(spacer(4));
        ct.addElement(buildQuote(story.optString("sarcasm", "")));

        ct.addElement(spacer(14));

        // ── Sources ───────────────────────────────────────────────────────────
        ct.addElement(new Paragraph(
                "SOURCES: " + story.optString("sources", ""),
                f(FontFactory.HELVETICA, 7.5f, Font.NORMAL, C_MUTED)));

        ct.go();
    }

    // ─── Panel (inset card block matching image blue panel) ──────────────────
    private static PdfPTable buildPanel(String label, String body,
                                        Color bg, Color labelColor, Color textColor) {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingLeft(14);
        cell.setPaddingRight(14);
        cell.setPaddingTop(12);
        cell.setPaddingBottom(12);

        // Badge inside card
        Paragraph lp = new Paragraph(label,
                f(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, labelColor));
        lp.setSpacingAfter(7);
        cell.addElement(lp);

        Paragraph bp = new Paragraph(body,
                f(FontFactory.HELVETICA, 10, Font.ITALIC, textColor));
        bp.setAlignment(Element.ALIGN_JUSTIFIED);
        bp.setLeading(15);
        cell.addElement(bp);

        card.addCell(cell);
        return card;
    }

    // ─── Quote block (sarcasm — gold left border, italic) ────────────────────
    private static PdfPTable buildQuote(String text) {
        PdfPTable t = new PdfPTable(new float[]{0.02f, 0.98f});
        t.setWidthPercentage(100);

        PdfPCell bar = new PdfPCell();
        bar.setBackgroundColor(C_GOLD);
        bar.setBorder(PdfPCell.NO_BORDER);
        t.addCell(bar);

        PdfPCell tc = new PdfPCell();
        tc.setBorder(PdfPCell.NO_BORDER);
        tc.setPaddingLeft(10);
        tc.setPaddingTop(3);
        tc.setPaddingBottom(3);
        Paragraph p = new Paragraph(text,
                f(FontFactory.HELVETICA, 10, Font.ITALIC, C_TEXT_MID));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setLeading(14.5f);
        tc.addElement(p);
        t.addCell(tc);

        return t;
    }

    // ─── Small badge label ────────────────────────────────────────────────────
    private static Paragraph smallBadge(String text, Color color) {
        Paragraph p = new Paragraph(text,
                f(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, color));
        p.setSpacingBefore(2);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BIGGEST STORIES
    // ═════════════════════════════════════════════════════════════════════════
    private static void addBiggestPage(Document doc, PdfWriter writer, JSONObject biggest) throws Exception {
        paintBg(writer);
        paintTopBar(writer, "TODAY'S BIGGEST STORIES", C_GOLD);
        doc.add(spacer(44));

        String[] keys   = {"global", "nepal", "tech"};
        String[] labels = {"GLOBAL TOP STORY", "NEPAL TOP STORY", "TECH TOP STORY"};
        Color[]  colors = SECTION_ACCENT;

        for (int i = 0; i < keys.length; i++) {
            if (!biggest.has(keys[i])) continue;
            JSONObject s = biggest.getJSONObject(keys[i]);

            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            t.setSpacingBefore(14);

            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(C_CARD);
            cell.setBorder(PdfPCell.NO_BORDER);
            cell.enableBorderSide(PdfPCell.LEFT);
            cell.setBorderColorLeft(colors[i]);
            cell.setBorderWidthLeft(5f);
            cell.setPadding(14);

            cell.addElement(new Paragraph("★  " + labels[i],
                    f(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, colors[i])));
            cell.addElement(spacer(4));
            cell.addElement(new Paragraph(s.optString("headline"),
                    f(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, C_TEXT_LITE)));
            cell.addElement(spacer(5));
            cell.addElement(new Paragraph(s.optString("summary"),
                    f(FontFactory.TIMES_ROMAN, 10, Font.NORMAL, C_TAN)));

            t.addCell(cell);
            doc.add(t);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PAGE BG + TOP BAR
    // ═════════════════════════════════════════════════════════════════════════
    private static void paintBg(PdfWriter writer) {
        PdfContentByte cb = writer.getDirectContentUnder();
        float w = PageSize.A4.getWidth();
        float h = PageSize.A4.getHeight();
        cb.setColorFill(C_PAGE_BG);
        cb.rectangle(0, 0, w, h);
        cb.fill();
    }

    private static void paintTopBar(PdfWriter writer, String text, Color barColor) {
        PdfContentByte cb = writer.getDirectContent();
        float w = PageSize.A4.getWidth();
        float h = PageSize.A4.getHeight();
        cb.setColorFill(barColor);
        cb.rectangle(0, h - 38, w, 38);
        cb.fill();
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(text, f(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, C_TEXT_DARK)),
                w / 2, h - 23, 0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FOOTER
    // ═════════════════════════════════════════════════════════════════════════
    static class FooterPainter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            float w = PageSize.A4.getWidth();

            cb.setColorStroke(C_RULE);
            cb.setLineWidth(0.5f);
            cb.moveTo(50, 56);
            cb.lineTo(w - 50, 56);
            cb.stroke();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Daily Intelligence Report",
                            FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, C_MUTED)),
                    50, 42, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.valueOf(writer.getPageNumber()),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, C_TEXT_MID)),
                    w / 2, 42, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Personal Use Only",
                            FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, C_MUTED)),
                    w - 50, 42, 0);

            cb.setColorFill(C_GOLD);
            cb.rectangle(0, 0, w, 5);
            cb.fill();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SPACER
    // ═════════════════════════════════════════════════════════════════════════
    private static Element spacer(float h) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(h);
        return p;
    }
}