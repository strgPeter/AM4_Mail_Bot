package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class Scraper {
    private static final Logger logger = LoggerFactory.getLogger(Scraper.class);
    private static final String URL = "https://airlinemanager.com";

    static Optional<ScrapeResult> getData() {
        logger.info("Starting scrape operation");
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--headless", "--disable-gpu", "--window-size=1080,720", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        try {
            int attempts = 3;
            while (attempts > 0) {
                try {
                    driver.get(URL);
                    logger.info("Initial page loaded: {}", driver.getTitle());
                    break;
                } catch (Exception e) {
                    attempts--;
                    if (attempts == 0) throw e;
                    logger.warn("Page load failed, retrying... Attempts left: {}", attempts);
                    Thread.sleep(5000);
                }
            }

            login(driver);

            if (isInitial(driver)) throw new IllegalStateException("Login failed after all attempts");

            return Optional.of(getPrices(driver));
        } catch (IllegalStateException e) {
            logger.error("Login failed", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Scrape operation failed", e);
            return Optional.empty();
        } finally {
            driver.quit();
            logger.info("WebDriver session closed");
        }
    }

    private static void login(WebDriver driver) {
        int loginAttempts = 3;

        do {
            logger.info("Login attempt {}", 4 - loginAttempts);
            try {
                WebElement playButton = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div[1]/div/div[1]/button[1]")));
                if (playButton == null) throw new NoSuchElementException("Could not find 'Play Free Now' button");
                playButton.click();

                WebElement loginOnRegistrationForm = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[3]/div[3]/div/form/div[2]/button")));
                if (loginOnRegistrationForm == null) throw new NoSuchElementException("Could not find login button on registration form");
                loginOnRegistrationForm.click();

                WebElement form = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[4]/div[3]/div/form")));
                if (form == null) throw new NoSuchElementException("Could not find login form");

                Map.Entry<String, String> credentials = getCredentials();
                WebElement emailInput = form.findElement(By.name("lEmail"));
                emailInput.sendKeys(credentials.getKey());
                WebElement passwordInput = form.findElement(By.name("lPass"));
                passwordInput.sendKeys(credentials.getValue());

                WebElement loginOnLoginForm = form.findElement(By.id("btnLogin"));
                loginOnLoginForm.click();

                new WebDriverWait(driver, Duration.ofSeconds(20))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("mapMaint")));
                logger.info("Login successful");
                break;
            } catch (TimeoutException e) {
                logger.warn("Login attempt timed out");
                loginAttempts--;
                if (loginAttempts == 0) throw new IllegalStateException("All login attempts failed");
            }
            catch (Exception e) {
                logger.warn("Login attempt failed", e);
                loginAttempts--;
                if (loginAttempts == 0) throw new IllegalStateException("All login attempts failed");
            }
        } while (loginAttempts > 0);
    }

    private static ScrapeResult getPrices(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.preloader")));

        WebElement resourcesBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("mapMaint")));
        resourcesBtn.click();

        WebElement fuelPriceElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[9]/div/div/div[3]/div[2]/div/div[1]/span[2]/b")));
        String fuelPriceText = fuelPriceElement.getText();
        logger.info("Raw Fuel Price Text: {}", fuelPriceText);

        WebElement co2btn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("popBtn2")));
        co2btn.click();

        WebElement co2PriceElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[9]/div/div/div[3]/div[2]/div/div/div[2]/span[2]/b")));
        String co2PriceText = co2PriceElement.getText();
        logger.info("Raw Co2 Price Text: {}", co2PriceText);

        return new ScrapeResult(fuelPriceText, co2PriceText);
    }

    private static boolean isInitial(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("mapMaint")));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static Map.Entry<String, String> getCredentials() {
        String email = System.getenv("AIRLINE_MANAGER_EMAIL");
        String password = System.getenv("AIRLINE_MANAGER_PASSWORD");
        if (email == null || password == null) {
            logger.error("EMAIL and PASSWORD environment variables are not set");
            throw new IllegalStateException("EMAIL and PASSWORD environment variables are not set");
        }
        logger.debug("Credentials retrieved successfully");
        return new AbstractMap.SimpleImmutableEntry<>(email, password);
    }
}