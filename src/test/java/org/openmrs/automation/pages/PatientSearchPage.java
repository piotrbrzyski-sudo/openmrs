package org.openmrs.automation.pages;

import java.util.List;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public final class PatientSearchPage extends BasePage {
    private static final By SEARCH = By.id("patient-search");
    private static final By PROCESSING = By.id("patient-search-results-table_processing");
    private static final By RESULT_ROWS =
            By.cssSelector("#patient-search-results-table tbody tr");

    public PatientSearchPage(DriverSession driverSession, TestConfig config) {
        super(driverSession, config);
    }

    public void searchAndOpen(String searchTerm) {
        type(SEARCH, searchTerm);
        if (isDisplayed(PROCESSING)) {
            waitUntilInvisible(PROCESSING);
        }

        WebElement matchingRow = waitFor().until(webDriver -> {
            List<WebElement> rows = webDriver.findElements(RESULT_ROWS);
            return rows.stream()
                    .filter(WebElement::isDisplayed)
                    .filter(row -> row.getText().toLowerCase().contains(searchTerm.toLowerCase()))
                    .findFirst()
                    .orElse(null);
        });

        matchingRow.click();
        waitForUrlToContain("patient.page?patientId=");
    }
}
