package org.example;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // --- Configuration ---
    private static final int FUEL_THRESHOLD = 800;
    private static final int CO2_THRESHOLD = 130;
    private static final long TASK_INTERVAL_MINUTES = 30; // Run every 30 minutes

    // Define the operating time window
    private static final LocalTime START_TIME = LocalTime.of(6, 0); // Earliest run starts at or after 06:00 (so 06:01 check is included)
    private static final LocalTime END_TIME = LocalTime.of(23, 1); // Latest run must START at or before 23:01

    public static void main(String[] args) {
        logger.info("Application started");

        // --- Environment Variable Loading with Validation ---
        String recipient1 = System.getenv("MAIL_RECIPIENT_1");
        if (recipient1 == null || recipient1.isBlank()) {
            logger.error("Mandatory environment variable MAIL_RECIPIENT_1 is not set. Exiting.");
            System.exit(1);
        }
        // Using a Set ensures no duplicates if more recipients are added later
        final Set<String> MAIL_RECIPIENTS = Set.copyOf(Set.of(recipient1));
        final String MAIL_ADMIN = recipient1;

        logger.info("Configuration loaded. Fuel Threshold: {}, CO2 Threshold: {}, Recipients: {}, Admin: {}",
                FUEL_THRESHOLD, CO2_THRESHOLD, MAIL_RECIPIENTS, MAIL_ADMIN);

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

        // --- Calculate Initial Delay ---
        long initialDelayMillis = calculateInitialDelayMillis();
        logger.info("Calculated initial delay: {} minutes ({} ms)", TimeUnit.MILLISECONDS.toMinutes(initialDelayMillis) , initialDelayMillis);


        // --- Schedule Task ---
        ses.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();

            if (currentTime.isBefore(START_TIME) || currentTime.isAfter(END_TIME)) {
                logger.info("Skipping task execution at {} - Outside operating hours ({}-{})",
                        currentTime, START_TIME, END_TIME);
                return;
            }

            logger.info("Task started at {}", now);
            try {
                Optional<ScrapeResult> optionalScraperResult = Scraper.getData();

                if (optionalScraperResult.isEmpty()) {
                    try {
                        MailClient.send(
                                "Scraping Error: Empty Result",
                                "Failed to retrieve data from scraper (returned empty Optional). Please check scraper status.",
                                MAIL_ADMIN
                        );
                        logger.warn("Scrape failed (empty result), notified admin {}", MAIL_ADMIN);
                    } catch (MessagingException e) {
                        logger.error("Failed to send admin mail about empty scrape result", e);
                    }
                    return;
                }

                ScrapeResult scraperResult = optionalScraperResult.get();
                int f = scraperResult.getFuel();
                int c = scraperResult.getCo2();

                logger.info("Scraped Data -> Fuel: {}, CO2: {}", f, c);

                String baseMsg = """
                    Fuel price = %d
                    Co2 price = %d

                    --------------------
                    This message was sent by an automated mail bot by Peter.
                    (https://github.com/strgPeter)
                    Timestamp: %s
                    """.formatted(f, c, LocalDateTime.now());

                String subject = null;

                // --- Threshold Checks and Notification Logic ---
                boolean fuelLow = f <= FUEL_THRESHOLD;
                boolean co2Low = c <= CO2_THRESHOLD;

                if (fuelLow && co2Low) {
                    subject = String.format("ALERT: Fuel: %d | CO2: %d", f, c);
                    logger.info("Condition matched: Both Fuel and CO2 below threshold.");
                } else if (fuelLow) {
                    subject = String.format("ALERT: Fuel: %d", f);
                    logger.info("Condition matched: Fuel below threshold.");
                } else if (co2Low) {
                    subject = String.format("ALERT: CO2: %d", c);
                    logger.info("Condition matched: CO2 below threshold.");
                } else {
                    logger.info("No thresholds met (Fuel: {}, CO2: {}). No notification sent.", f, c);
                }

                if (subject != null) {
                    try {
                        MailClient.send(subject, baseMsg, MAIL_RECIPIENTS);
                        logger.info("Sent notification email to {}. Subject: {}", MAIL_RECIPIENTS, subject);
                    } catch (MessagingException e) {
                        logger.error("Failed to send threshold notification mail to {}", MAIL_RECIPIENTS, e);
                    }
                }

            } catch (Exception e) {
                logger.error("Unexpected error during task execution", e);
            }

        }, initialDelayMillis, TimeUnit.MINUTES.toMillis(TASK_INTERVAL_MINUTES), TimeUnit.MILLISECONDS);

        logger.info("Task scheduled to run every {} minutes, starting in ~{} minutes.",
                TASK_INTERVAL_MINUTES, TimeUnit.MILLISECONDS.toMinutes(initialDelayMillis));

        // --- Shutdown Hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown requested. Shutting down executor...");
            ses.shutdown();
            try {
                if (!ses.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in time. Forcing shutdown...");
                    ses.shutdownNow();
                    if (!ses.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("Executor did not terminate even after forcing.");
                    }
                } else {
                    logger.info("Executor terminated gracefully.");
                }
            } catch (InterruptedException e) {
                logger.error("Shutdown interrupted while waiting for executor termination. Forcing shutdown.", e);
                ses.shutdownNow();
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
            logger.info("Shutdown complete.");
        }));
    }

    /**
     * Calculates the delay in milliseconds until the next hh:01 or hh:31 mark.
     *
     * @return Delay in milliseconds.
     */
    private static long calculateInitialDelayMillis() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nextRun;
        int currentMinute = now.getMinute();
        int currentSecond = now.getSecond();

        LocalDateTime baseTime = now.truncatedTo(ChronoUnit.HOURS);

        if (currentMinute < 1) {
            // If before hh:01, next run is hh:01
            nextRun = baseTime.withMinute(1);
        } else if (currentMinute < 31) {
            // If after hh:01 but before hh:31, next run is hh:31
            nextRun = baseTime.withMinute(31);
        } else {
            // If after hh:31, next run is (hh+1):01
            nextRun = baseTime.plusHours(1).withMinute(1);
        }

        // Ensure nextRun is in the future (handles edge case if calculation ends up being 'now')
        if (!nextRun.isAfter(now)) {
            // This typically happens if 'now' is exactly xx:01:00 or xx:31:00
            // We need to find the *next* slot after the current one.
            if (nextRun.getMinute() == 1) {
                nextRun = baseTime.withMinute(31); // Jump to xx:31
            } else { // Must have been xx:31
                nextRun = baseTime.plusHours(1).withMinute(1); // Jump to (xx+1):01
            }
        }

        Duration initialDelay = Duration.between(now, nextRun);

        return Math.max(0, initialDelay.toMillis());
    }
}