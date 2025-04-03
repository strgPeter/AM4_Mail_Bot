package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrapeResult {
    private static final Logger logger = LoggerFactory.getLogger(ScrapeResult.class);
    private int fuel;
    private int co2;

    public ScrapeResult(String fuel, String co2) {
        try {
            this.fuel = Integer.parseInt(fuel.replace("$", "").replace(",", "").trim());
            this.co2 = Integer.parseInt(co2.replace("$", "").replace(",", "").trim());
            logger.debug("Parsed fuel: {}, co2: {}", this.fuel, this.co2);
        } catch (NumberFormatException e) {
            this.fuel = -1;
            this.co2 = -1;
            logger.warn("Failed to parse prices - fuel: '{}', co2: '{}'", fuel, co2);
        }
    }

    public int getFuel() {
        return fuel;
    }

    public int getCo2() {
        return co2;
    }
}