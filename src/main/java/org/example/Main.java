package org.example;

import jakarta.mail.MessagingException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        final int FUEL_THRESHOLD = 600;
        final int CO2_THRESHOLD = 120;
        final Set<String> MAIL_RECIPIENTS = Set.of(System.getenv("MAIL_RECIPIENT_1"));
        final String MAIL_ADMIN = System.getenv("MAIL_RECIPIENT_1");

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(() -> {
            Optional<ScrapeResult> optionalScraperResult = Scraper.getData();

            if (optionalScraperResult.isEmpty()) {
                try {
                    MailClient.send(
                            "Something wend wrong retrieving data - Empty optional",
                            "Admin Mail",
                            MAIL_ADMIN
                    );
                } catch (MessagingException e) {
                    System.out.println("Could not send mail to admin!");
                }
                return;
            }

            ScrapeResult scraperResult = optionalScraperResult.get();

            int f = scraperResult.getFuel();
            int c = scraperResult.getCo2();

            String msg = """
                    Fuel price = %d
                    Co2 price = %d
                    This message was sent by an automated mail bot by Peter.
                    (https://github.com/strgPeter)
                    """.formatted(f, c);

            try {
                if (f <= FUEL_THRESHOLD && c > CO2_THRESHOLD) {
                    MailClient.send(String.format("Fuel: %d", f), msg, MAIL_RECIPIENTS);
                    System.out.println("Sent fuel notification");
                } else if (c <= CO2_THRESHOLD && f > FUEL_THRESHOLD) {
                    MailClient.send(String.format("Co2: %d", c), msg, MAIL_RECIPIENTS);
                    System.out.println("Sent co2 notification");
                } else if (c <= CO2_THRESHOLD && f <= FUEL_THRESHOLD) {
                    MailClient.send(String.format("Fuel: %d; Co2: %d", f, c), msg, MAIL_RECIPIENTS);
                    System.out.println("Sent fuel and co2 notification");
                }
            } catch (MessagingException e) {
                System.out.println("An error occurred while sending mail notification");
                System.out.println(e.getMessage());
            }

        }, 0, 30, TimeUnit.MINUTES);

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down executor...");
            ses.shutdown();
            try {
                if (!ses.awaitTermination(5, TimeUnit.SECONDS)) {
                    ses.shutdownNow();
                }
            } catch (InterruptedException e) {
                ses.shutdownNow();
            }
        }));
    }
}