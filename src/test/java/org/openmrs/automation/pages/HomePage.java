package org.openmrs.automation.pages;

import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openqa.selenium.By;

public final class HomePage extends BasePage {
    private static final By FIND_PATIENT = By.id(
            "coreapps-activeVisitsHomepageLink-coreapps-activeVisitsHomepageLink-extension");
    private static final By FIND_PATIENT_BY_URL =
            By.cssSelector("a[href*='findpatient/findPatient.page']");
    private static final By REGISTER_PATIENT = By.id(
            "referenceapplication-registrationapp-registerPatient-homepageLink-"
                    + "referenceapplication-registrationapp-registerPatient-homepageLink-extension");
    private static final By REGISTER_PATIENT_BY_URL =
            By.cssSelector("a[href*='registrationapp/registerPatient.page']");

    public HomePage(DriverSession driverSession, TestConfig config) {
        super(driverSession, config);
    }

    public void openPatientSearch() {
        click(FIND_PATIENT, FIND_PATIENT_BY_URL);
        waitForUrlToContain("findPatient.page");
    }

    public void openPatientRegistration() {
        click(REGISTER_PATIENT, REGISTER_PATIENT_BY_URL);
        waitForUrlToContain("registerPatient.page");
    }
}
