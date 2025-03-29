package org.example;

public class ScrapeResult {
    private String s_fuel;
    private String s_co2;

    private int fuel;
    private int co2;

    public ScrapeResult(String fuel, String co2) {
        s_fuel = fuel;
        s_co2 = co2;

        this.fuel = Integer.parseInt(s_fuel.replace("$", "").replace(",", "").trim());
        this.co2 = Integer.parseInt(s_co2.replace("$", "").replace(",", "").trim());
    }

    public int getFuel() {
        return fuel;
    }
    public int getCo2() {
        return co2;
    }
}
