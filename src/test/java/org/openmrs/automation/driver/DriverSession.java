package org.openmrs.automation.driver;

import java.time.Duration;
import org.openmrs.automation.config.TestConfig;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public final class DriverSession {
    private final TestConfig config;
    private WebDriver driver;

    public DriverSession(TestConfig config) {
        this.config = config;
        configureSeleniumManager();
    }

    public WebDriver driver() {
        if (driver == null) {
            driver = createDriver();
            Duration timeout = config.uiTimeout();
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.manage().timeouts().pageLoadTimeout(timeout.plusSeconds(20));
            driver.manage().timeouts().scriptTimeout(timeout);
        }
        return driver;
    }

    public boolean isStarted() {
        return driver != null;
    }

    public byte[] screenshot() {
        if (!isStarted() || !(driver instanceof TakesScreenshot screenshotDriver)) {
            return new byte[0];
        }
        return screenshotDriver.getScreenshotAs(OutputType.BYTES);
    }

    public void quit() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }

    private WebDriver createDriver() {
        return switch (config.browser()) {
            case "chrome" -> new ChromeDriver(chromeOptions());
            case "firefox" -> new FirefoxDriver(firefoxOptions());
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '%s'. Use chrome or firefox.".formatted(config.browser()));
        };
    }

    private void configureSeleniumManager() {
        if (System.getProperty("SE_SKIP_DRIVER_IN_PATH") == null
                && System.getenv("SE_SKIP_DRIVER_IN_PATH") == null) {
            System.setProperty("SE_SKIP_DRIVER_IN_PATH", "true");
        }
    }

    private ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--disable-dev-shm-usage",
                "--disable-notifications",
                "--no-sandbox",
                "--window-size=1920,1080");
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    private FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1920", "--height=1080");
        if (config.headless()) {
            options.addArguments("-headless");
        }
        return options;
    }
}
