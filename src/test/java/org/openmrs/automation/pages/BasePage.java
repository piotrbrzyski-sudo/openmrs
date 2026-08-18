package org.openmrs.automation.pages;

import java.util.Arrays;
import java.util.List;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

abstract class BasePage {
    private final DriverSession driverSession;
    protected final TestConfig config;

    BasePage(DriverSession driverSession, TestConfig config) {
        this.driverSession = driverSession;
        this.config = config;
    }

    protected WebDriver driver() {
        return driverSession.driver();
    }

    protected WebDriverWait waitFor() {
        return new WebDriverWait(driver(), config.uiTimeout());
    }

    protected void open(String relativePath) {
        driver().navigate().to(config.uiBaseUrl() + relativePath);
        waitForDocumentReady();
    }

    protected void waitForDocumentReady() {
        waitFor().ignoring(StaleElementReferenceException.class).until(webDriver -> {
            Object ready = ((JavascriptExecutor) webDriver).executeScript(
                    "return document.readyState === 'complete' "
                            + "&& (typeof window.jQuery === 'undefined' || window.jQuery.active === 0);");
            return Boolean.TRUE.equals(ready);
        });
    }

    protected WebElement visible(By locator) {
        return waitFor().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement visible(By... alternatives) {
        return waitFor().until(webDriver -> Arrays.stream(alternatives)
                .flatMap(locator -> webDriver.findElements(locator).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElse(null));
    }

    protected WebElement clickable(By locator) {
        return waitFor().until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected WebElement clickable(By... alternatives) {
        return waitFor().until(webDriver -> Arrays.stream(alternatives)
                .flatMap(locator -> webDriver.findElements(locator).stream())
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .findFirst()
                .orElse(null));
    }

    protected WebElement editable(By locator) {
        return waitFor().ignoring(StaleElementReferenceException.class).until(webDriver -> webDriver
                .findElements(locator)
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(element -> !Boolean.parseBoolean(element.getDomProperty("readOnly")))
                .findFirst()
                .orElse(null));
    }

    protected void click(By locator) {
        clickElement(clickable(locator));
    }

    protected void click(By... alternatives) {
        clickElement(clickable(alternatives));
    }

    protected void type(By locator, String value) {
        WebElement element = editable(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected void select(By locator, String visibleText) {
        new Select(visible(locator)).selectByVisibleText(visibleText);
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected List<WebElement> visibleElements(By locator) {
        return waitFor().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected void waitUntilInvisible(By locator) {
        waitFor().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForUrlToContain(String value) {
        waitFor().until(ExpectedConditions.urlContains(value));
        waitForDocumentReady();
    }

    protected boolean isDisplayed(By locator) {
        return driver().findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    protected String currentUrl() {
        return driver().getCurrentUrl();
    }

    protected void refresh() {
        driver().navigate().refresh();
        waitForDocumentReady();
    }

    private void clickElement(WebElement element) {
        ((JavascriptExecutor) driver())
                .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        element.click();
    }
}
