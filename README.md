# Daily Intelligence Report

A self-hosted, fully automated daily news mailer that generates a beautiful PDF briefing and sends it to your inbox every morning — powered by **Gemini AI** and **GitHub Actions**. Zero cost. Zero maintenance. Just open your email.

---

## What It Does

Every day at 6:00 AM Nepal Time, this app:

1. Calls the Gemini API with a structured prompt
2. Gets back 30 curated news stories in JSON format (10 Global · 10 Nepal · 10 Tech)
3. Renders them into a polished, multi-page PDF with a cream/gold editorial design
4. Emails the PDF to your inbox and deletes the local file

No server. No database. No cloud bill. Just GitHub Actions running on a cron schedule.

---


## Tech Stack

| Layer | Tool |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot |
| AI | Google Gemini 2.5 Flash |
| PDF | OpenPDF (iText fork) |
| Email | Jakarta Mail (Gmail SMTP) |
| Scheduler | GitHub Actions (cron) |
| HTTP | OkHttp3 |
| JSON | org.json |

---

## Setup

### 1. Fork or clone this repo

```bash
git clone https://github.com/SandeshKhatiwada05/daily-news-mailer-spring-boot
cd daily-news-mailer
```

### 2. Get a Gemini API key

Go to [Google AI Studio](https://aistudio.google.com/app/apikey) and create a free API key.

Gemini 2.5 Flash free tier: **250 requests/day** — more than enough for one daily email.

### 3. Set up Gmail App Password

Gmail requires an App Password (not your real password) for SMTP access.

1. Enable 2-Factor Authentication on your Google account
2. Go to [Google Account → Security → App Passwords](https://myaccount.google.com/apppasswords)
3. Create an app password for "Mail"
4. Copy the 16-character password

### 4. Add GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**

Add these three secrets:

| Secret name | Value |
|---|---|
| `GEMINI_API_KEY` | Your Gemini API key |
| `MAIL_USERNAME` | Your Gmail address (e.g. `you@gmail.com`) |
| `MAIL_PASSWORD` | Your 16-char Gmail App Password |

### 5. Enable GitHub Actions

Go to your repo → **Actions** tab → enable workflows if prompted.

The workflow runs automatically at **00:00 UTC (06:00 NPT)** every day.

To test it manually: Actions → **Daily News** → **Run workflow**.

---

## Configuration

### Change delivery time

Edit `.github/workflows/daily-news.yml`:

```yaml
on:
  schedule:
    - cron: '0 0 * * *'   # 00:00 UTC = 06:00 NPT
```

Cron is always UTC. Use [crontab.guru](https://crontab.guru) to convert your timezone.

### Change news focus

Edit `Prompt.java` → `DAILY_NEWS_JSON` to adjust sections, story count, tone, or language.

---

## Project Structure

```
src/main/java/com/personal_news_prortal/newsportal/
├── NewsportalApplication.java   # Entry point, wires everything together
├── GeminiService.java           # Calls Gemini API, parses JSON response
├── PdfBuilder.java              # Renders the beautiful PDF from parsed data
├── EmailService.java            # Sends PDF via Gmail SMTP
└── Prompt.java                  # The prompt sent to Gemini

.github/workflows/
└── daily-news.yml               # GitHub Actions cron job
```

---

## Is This Safe to Make Public?

**Yes.** All sensitive values (API key, email, password) are stored as **GitHub Actions Secrets** — they are never written into the code or committed to the repo. The workflow references them as `${{ secrets.SECRET_NAME }}`, which GitHub injects at runtime only.

What's safe to be public: the code, the prompt, the PDF design — none of these contain credentials.

---

## Cost

| Service | Cost |
|---|---|
| GitHub Actions | Free (2,000 min/month on free tier; this job uses ~2 min/day) |
| Gemini 2.5 Flash API | Free (250 req/day limit; uses 1/day) |
| Gmail SMTP | Free |

**Total: $0/month.**

---

## Dependencies (pom.xml)

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>

<!-- OpenPDF -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.30</version>
</dependency>

<!-- OkHttp -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20240303</version>
</dependency>

<!-- Jakarta Mail -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>2.0.1</version>
</dependency>
```

*Built because manually reading 30 news tabs every morning is a crime against productivity.*
