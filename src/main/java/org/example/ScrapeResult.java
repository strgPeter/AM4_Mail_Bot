package org.example;

public class ScrapeResult {
    private String s_fuel;
    private String s_co2;

    private int fuel;
    private int co2;

    public ScrapeResult(String fuel, String co2) {
        s_fuel = fuel;
        s_co2 = co2;
    }

    private void parse(){
        fuel = Integer.parseInt(s_fuel.substring(s_fuel.indexOf(' ')));
    }

    public int getFuel() {
        return fuel;
    }
    public int getCo2() {
        return co2;
    }
}
