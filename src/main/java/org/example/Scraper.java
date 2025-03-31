package org.example;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

public class Scraper {

    private final static String URL = "https://airlinemanager.com";

    static Optional<ScrapeResult> getData() {

        Optional<ScrapeResult> result = Optional.empty();

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1080,720");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(URL);
            System.out.println("Initial page loaded: " + driver.getTitle());

            login(driver);

            if (isInitial(driver)) throw new IllegalStateException();

            result = Optional.of(getPrices(driver));

        }catch (IllegalStateException ise) {
            System.out.println("Login failed: " + ise.getMessage());
            result = Optional.empty();
        }catch (Exception e) {
            System.out.println("Something went wrong: " + e.getMessage());
            result = Optional.empty();
        } finally {
            driver.quit();
        }
        return result;
    }

    private static void login(WebDriver driver){
        int loginAttempts = 3;

        do {
            System.out.printf("Attempt %d - ", 4 - loginAttempts);

            // Click "Play Free Now" button
            WebElement playButton = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div[1]/div/div[1]/button[1]")));
            if (playButton == null) throw new NoSuchElementException("Could not find \"play free now\" button!");
            playButton.click();

            // Click "Log in" button on registration form
            WebElement loginOnRegistrationForm = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[3]/div[3]/div/form/div[2]/button")));
            if (loginOnRegistrationForm == null) throw new NoSuchElementException("Could not find login button on registration form!");
            loginOnRegistrationForm.click();

            // Wait for the login form to be visible
            WebElement form = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[4]/div[3]/div/form")));
            if (form == null) throw new NoSuchElementException("Could not find login form!");

            // Fill credentials
            Map.Entry<String, String> c = getCredentials();
            WebElement emailInput = form.findElement(By.name("lEmail"));
            emailInput.sendKeys(c.getKey());
            WebElement passwordInput = form.findElement(By.name("lPass"));
            passwordInput.sendKeys(c.getValue());

            // Click "Log In" button using its ID
            WebElement loginOnLoginForm = form.findElement(By.id("btnLogin"));
            loginOnLoginForm.click();

            // Wait for page to load after login (improve this later if needed)
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            loginAttempts--;

            if (isInitial(driver)){
                System.out.println("failed");
            } else {
                System.out.println("success");
                break;
            }

        } while (loginAttempts > 0);
    }

    private static ScrapeResult getPrices(WebDriver driver){
        WebDriverWait www = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Wait for preloader to disappear
        www.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.preloader")));

        WebElement resourcesBtn = www.until(ExpectedConditions.elementToBeClickable(By.id("mapMaint")));
        resourcesBtn.click();

        WebElement fuelPriceElement = www.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[9]/div/div/div[3]/div[2]/div/div[1]/span[2]/b")));
        String fuelPriceText = fuelPriceElement.getText();
        System.out.println("Raw Fuel Price Text: " + fuelPriceText);

        WebElement co2btn = www.until(ExpectedConditions.visibilityOfElementLocated(By.id("popBtn2")));
        co2btn.click();

        WebElement co2PriceElement = www.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[9]/div/div/div[3]/div[2]/div/div/div[2]/span[2]/b")));
        String co2PriceText = co2PriceElement.getText();
        System.out.println("Raw Co2 Price Text: " + co2PriceText);

        return new ScrapeResult(fuelPriceText, co2PriceText);
    }

    private static Boolean isInitial(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("mapMaint")));
        }catch (Exception e) {
            return true;
        }
        return false;
    }

    private static Map.Entry<String, String> getCredentials () throws IllegalStateException{
        String email = System.getenv("AIRLINE_MANAGER_EMAIL");
        String password = System.getenv("AIRLINE_MANAGER_PASSWORD");

        if (email == null || password == null) throw new IllegalStateException("EMAIL and PASSWORD environment variables are not set");

        return new AbstractMap.SimpleImmutableEntry<>(email, password);
    }

}
