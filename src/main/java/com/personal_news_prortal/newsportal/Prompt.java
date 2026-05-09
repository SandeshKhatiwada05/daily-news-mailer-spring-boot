package com.personal_news_prortal.newsportal;

public class Prompt {
    public Prompt() {
    }

    /**
     * JSON-structured prompt. Free-tier Gemini returns clean JSON → Java builds beautiful PDF.
     * No markdown parsing needed. Full layout control.
     */
    public static final String DAILY_NEWS_JSON = """
            You are a world-class news analyst producing a structured daily intelligence briefing.
            
            Return ONLY a valid JSON object. No markdown. No backticks. No preamble. No explanation.
            
            The JSON must follow this EXACT structure:
            
            {
              "date": "May 9, 2025",
              "sections": [
                {
                  "id": "global",
                  "title": "GLOBAL NEWS",
                  "stories": [
                    {
                      "headline": "Story headline here",
                      "what_happened": "150-200 word explanation: what, who, why it matters, background, consequences. Explain jargon simply.",
                      "plain_english": "40-80 word plain English summary starting with: Here is what this actually means —",
                      "sarcasm": "2-3 sentence dry/cynical/ironic commentary. Intelligent, not juvenile.",
                      "sources": "Source Name, Date — Confirmed/Developing"
                    }
                  ]
                }
              ],
              "biggest_stories": {
                "global": {
                  "headline": "Biggest global story headline",
                  "summary": "2-3 sentence summary"
                },
                "nepal": {
                  "headline": "Biggest Nepal story headline",
                  "summary": "2-3 sentence summary"
                },
                "tech": {
                  "headline": "Biggest tech story headline",
                  "summary": "2-3 sentence summary"
                }
              }
            }
            
            REQUIREMENTS:
            
            Section 1 id="global", title="GLOBAL NEWS" — 10 stories
            Focus: geopolitics, wars, diplomacy, economy, disasters, major international developments.
            Avoid: celebrity gossip, internet drama.
            
            Section 2 id="nepal", title="NEPAL NEWS" — 10 stories
            Focus: politics, corruption, economy, infrastructure, transportation, public safety,
            energy, education, judiciary, major policy decisions.
            Always explain impact on ordinary Nepali citizens.
            
            Section 3 id="tech", title="TECHNOLOGY & CODING NEWS" — 10 stories
            Focus: AI, software engineering, cybersecurity, open source, cloud computing,
            programming languages, semiconductors, developer tools, big tech, major regulations.
            Always explain why developers should care.
            
            STRICT RULES:
            - Return EXACTLY 10 stories per section. Total = 30 stories.
            - Never fabricate facts. Use reputable sources only.
            - No markdown inside JSON string values.
            - All string values must be properly JSON-escaped.
            - If fewer important stories exist, include medium-priority but meaningful ones.
            - Do NOT truncate. Complete all 30 stories.
            """;

    // Keep original for reference
    public static final String DAILY_NEWS = """
            Generate a DOWNLOADABLE PDF FILE titled:
            "Daily Intelligence Report"
            ... (original prompt kept for reference)
            """;
}