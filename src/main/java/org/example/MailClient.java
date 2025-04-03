package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.Set;

public class MailClient {

    private static final String username = System.getenv("MAIL_SENDER_USN");
    private static final String password = System.getenv("MAIL_SENDER_PWD");

    public static void send(String subject, String text, Set<String> recipients) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(prop,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(String.join(", ", recipients))
        );
        message.setSubject(subject);
        message.setText(text);

        Transport.send(message);
    }

    public static void send(String subject, String text, String recipient) throws MessagingException {
        send(subject, text, Set.of(recipient));
    }
}
