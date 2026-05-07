package com.personal_news_prortal.newsportal;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Properties;

@Service
public class EmailService {

    public void sendMail(
            String username,
            String password,
            File attachment
    ) throws Exception {

        Properties props = new Properties();

        props.put("mail.smtp.auth",             "true");
        props.put("mail.smtp.starttls.enable",  "true");
        props.put("mail.smtp.host",             "smtp.gmail.com");
        props.put("mail.smtp.port",             "587");

        Session session = Session.getInstance(
                props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
        );

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(username));

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(username)
        );

        message.setSubject("Daily News");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Your daily news PDF is attached.");

        MimeBodyPart filePart = new MimeBodyPart();
        filePart.attachFile(attachment);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);

        message.setContent(multipart);

        Transport.send(message);
    }
}