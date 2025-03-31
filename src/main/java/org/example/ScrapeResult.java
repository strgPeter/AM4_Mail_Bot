package org.example;

public class ScrapeResult {

    private int fuel;
    private int co2;

    public ScrapeResult(String fuel, String co2) {

        try {
            this.fuel = Integer.parseInt(fuel.replace("$", "").replace(",", "").trim());
            this.co2 = Integer.parseInt(co2.replace("$", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            this.fuel = -1;
            this.co2 = -1;
        }
    }

    public int getFuel() {
        return fuel;
    }
    public int getCo2() {
        return co2;
    }
}
