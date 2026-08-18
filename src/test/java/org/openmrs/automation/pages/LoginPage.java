package org.openmrs.automation.pages;

import java.util.List;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

public final class LoginPage extends BasePage {
    private static final String LOGIN_FORM_READY = "login-form-ready";
    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By LOCATIONS = By.cssSelector("#sessionLocation li");
    private static final By LOGIN_BUTTON = By.id("loginButton");

    public LoginPage(DriverSession driverSession, TestConfig config) {
        super(driverSession, config);
    }

    public void login() {
        open("/login.htm");
        waitForLoginFormOrUpstreamFailure();
        type(USERNAME, config.username());
        type(PASSWORD, config.password());
        selectLocation(config.loginLocation());
        click(LOGIN_BUTTON);
        waitForUrlToContain("/referenceapplication/home.page");
    }

    private void selectLocation(String expectedLocation) {
        List<WebElement> locations;
        try {
            locations = visibleElements(LOCATIONS);
        } catch (TimeoutException timeout) {
            String blockingPage = upstreamBlockingPage();
            if (blockingPage != null) {
                throw blockingPageException(blockingPage, timeout);
            }
            throw timeout;
        }
        WebElement match = locations.stream()
                .filter(location -> expectedLocation.equalsIgnoreCase(location.getText().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Login location '%s' is not available. Visible locations: %s"
                                .formatted(
                                        expectedLocation,
                                        locations.stream()
                                                .map(WebElement::getText)
                                                .map(String::trim)
                                                .toList())));
        match.click();
    }

    private void waitForLoginFormOrUpstreamFailure() {
        String state = waitFor()
                .ignoring(StaleElementReferenceException.class)
                .until(webDriver -> {
                    String blockingPage = upstreamBlockingPage();
                    if (blockingPage != null) {
                        return blockingPage;
                    }
                    return isLoginFormAvailable() ? LOGIN_FORM_READY : null;
                });

        if (!LOGIN_FORM_READY.equals(state)) {
            throw blockingPageException(state, null);
        }
    }

    private boolean isLoginFormAvailable() {
        return List.of(USERNAME, PASSWORD, LOGIN_BUTTON).stream()
                .allMatch(locator -> driver().findElements(locator).stream()
                        .anyMatch(element -> element.isDisplayed() && element.isEnabled()));
    }

    private String upstreamBlockingPage() {
        String title = driver().getTitle().toLowerCase();
        String source = driver().getPageSource().toLowerCase();
        String body = driver().findElements(By.tagName("body")).stream()
                .findFirst()
                .map(WebElement::getText)
                .orElse("")
                .toLowerCase();

        if (title.contains("just a moment")
                || source.contains("cf-chl")
                || source.contains("challenges.cloudflare.com")
                || source.contains("cf-turnstile")
                || (title.contains("attention required") && source.contains("cloudflare"))) {
            return "Cloudflare browser challenge";
        }
        if (title.contains("502") || body.contains("502 bad gateway") || body.contains("bad gateway")) {
            return "HTTP 502 Bad Gateway";
        }
        if (title.contains("503")
                || body.contains("503 service unavailable")
                || body.contains("service unavailable")) {
            return "HTTP 503 Service Unavailable";
        }
        if (title.contains("maintenance")
                || body.contains("under maintenance")
                || body.contains("scheduled maintenance")
                || body.contains("temporarily unavailable")) {
            return "maintenance page";
        }
        if (body.contains("upstream connect error") || body.contains("upstream request timeout")) {
            return "upstream error page";
        }
        return null;
    }

    private IllegalStateException blockingPageException(String blockingPage, Throwable cause) {
        String message = "OpenMRS login is blocked by %s. URL: %s; title: %s"
                .formatted(blockingPage, driver().getCurrentUrl(), displayTitle());
        return cause == null
                ? new IllegalStateException(message)
                : new IllegalStateException(message, cause);
    }

    private String displayTitle() {
        String title = driver().getTitle().trim();
        return title.isEmpty() ? "<empty>" : "'" + title + "'";
    }
}
