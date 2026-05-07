package com.personal_news_prortal.newsportal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class NewsportalApplication implements ApplicationRunner {

	private final GeminiService geminiService;
	private final EmailService emailService;

	public NewsportalApplication(
			GeminiService geminiService,
			EmailService emailService
	) {
		this.geminiService = geminiService;
		this.emailService = emailService;
	}

	public static void main(String[] args) {
		SpringApplication.run(NewsportalApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String geminiKey = System.getenv("GEMINI_API_KEY");
		String gmail     = System.getenv("MAIL_USERNAME");
		String password  = System.getenv("MAIL_PASSWORD");

		File pdf = geminiService.generatePdf(geminiKey);

		emailService.sendMail(gmail, password, pdf);

		pdf.delete();

		System.out.println("Done");
	}
}