package org.openmrs.automation.pages;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public final class PatientDashboardPage extends BasePage {
    private static final Pattern BIRTH_DATE_IN_SUMMARY =
            Pattern.compile("\\((?:~)?\\s*([^()]+)\\)");
    private static final List<DateTimeFormatter> BIRTH_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd.MMM.uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE);

    private static final By GIVEN_NAME = By.cssSelector(".PersonName-givenName");
    private static final By FAMILY_NAME = By.cssSelector(".PersonName-familyName");
    private static final By GENDER = By.cssSelector(".gender-age > span:nth-of-type(1)");
    private static final By AGE_AND_BIRTH_DATE =
            By.cssSelector(".gender-age > span:nth-of-type(2)");
    private static final By IDENTIFIERS = By.cssSelector(".identifiers");

    private static final By START_VISIT =
            By.id("org.openmrs.module.coreapps.createVisit");
    private static final By START_VISIT_BY_URL =
            By.cssSelector("a[href*='startVisit']");
    private static final By START_VISIT_DIALOG =
            By.id("quick-visit-creation-dialog");
    private static final By VISIT_TYPE_SELECT =
            By.cssSelector("#quick-visit-creation-dialog select");
    private static final By VISIT_TYPE_RADIOS =
            By.cssSelector("#quick-visit-creation-dialog input[type='radio']");
    private static final By CONFIRM_START_VISIT =
            By.id("start-visit-with-visittype-confirm");

    private static final By END_VISIT =
            By.cssSelector("a[href*='showEndVisitDialog']");
    private static final By END_VISIT_DIALOG = By.id("end-visit-dialog");
    private static final By CONFIRM_END_VISIT =
            By.cssSelector("#end-visit-dialog button.confirm");

    public PatientDashboardPage(DriverSession driverSession, TestConfig config) {
        super(driverSession, config);
    }

    public void open(String patientUuid) {
        super.open("/coreapps/clinicianfacing/patient.page?patientId=" + patientUuid);
        visible(FAMILY_NAME);
    }

    public String givenName() {
        return text(GIVEN_NAME);
    }

    public String familyName() {
        return text(FAMILY_NAME);
    }

    public String gender() {
        return text(GENDER);
    }

    public LocalDate birthDate() {
        String summary = text(AGE_AND_BIRTH_DATE);
        Matcher matcher = BIRTH_DATE_IN_SUMMARY.matcher(summary);
        String displayedBirthDate = null;
        while (matcher.find()) {
            displayedBirthDate = matcher.group(1).trim();
        }
        if (displayedBirthDate == null) {
            throw new IllegalStateException(
                    "Patient dashboard does not expose a birth date in: '" + summary + "'");
        }

        for (DateTimeFormatter formatter : BIRTH_DATE_FORMATS) {
            try {
                return LocalDate.parse(displayedBirthDate, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported OpenMRS display format.
            }
        }
        throw new IllegalStateException(
                "Unable to parse patient dashboard birth date: '" + displayedBirthDate + "'");
    }

    public String identifiers() {
        return text(IDENTIFIERS);
    }

    public void waitForFamilyNameToPropagate(String expectedFamilyName) {
        AtomicReference<String> lastFamilyName = new AtomicReference<>("<not read>");

        try {
            waitFor()
                    .pollingEvery(Duration.ofMillis(500))
                    .until(webDriver -> {
                        String currentFamilyName = webDriver.findElements(FAMILY_NAME).stream()
                                .filter(WebElement::isDisplayed)
                                .map(WebElement::getText)
                                .map(String::trim)
                                .findFirst()
                                .orElse("<not visible>");
                        lastFamilyName.set(currentFamilyName);
                        if (expectedFamilyName.equals(currentFamilyName)) {
                            return true;
                        }
                        refresh();
                        return false;
                    });
        } catch (TimeoutException exception) {
            throw new AssertionError(
                    ("Patient family name did not propagate before the %s timeout. "
                                    + "Expected: '%s'; last read: '%s'")
                            .formatted(
                                    config.uiTimeout(),
                                    expectedFamilyName,
                                    lastFamilyName.get()),
                    exception);
        }
    }

    public void startAndCloseVisit() {
        click(START_VISIT, START_VISIT_BY_URL);
        visible(START_VISIT_DIALOG);
        chooseVisitType(config.visitType());
        click(CONFIRM_START_VISIT);
        waitUntilInvisible(START_VISIT_DIALOG);
        visible(END_VISIT);

        click(END_VISIT);
        visible(END_VISIT_DIALOG);
        click(CONFIRM_END_VISIT);
        waitFor().until(ExpectedConditions.invisibilityOfElementLocated(END_VISIT));
    }

    public boolean hasActiveVisit() {
        return isDisplayed(END_VISIT);
    }

    public boolean canStartVisit() {
        return isDisplayed(START_VISIT) || isDisplayed(START_VISIT_BY_URL);
    }

    private void chooseVisitType(String expectedVisitType) {
        List<WebElement> selects = driver().findElements(VISIT_TYPE_SELECT);
        if (!selects.isEmpty()) {
            new Select(selects.get(0)).selectByVisibleText(expectedVisitType);
            return;
        }

        List<WebElement> radios = driver().findElements(VISIT_TYPE_RADIOS).stream()
                .filter(WebElement::isDisplayed)
                .toList();
        if (radios.isEmpty()) {
            return;
        }
        WebElement matchingRadio = radios.stream()
                .filter(radio -> radio.findElement(By.xpath(".."))
                        .getText()
                        .toLowerCase()
                        .contains(expectedVisitType.toLowerCase()))
                .findFirst()
                .orElseGet(() -> {
                    if (radios.size() == 1) {
                        return radios.get(0);
                    }
                    throw new IllegalStateException(
                            "Visit type '%s' is not available".formatted(expectedVisitType));
                });
        matchingRadio.click();
    }

}
