package org.openmrs.automation.pages;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.driver.DriverSession;
import org.openmrs.automation.model.PatientProfile;
import org.openqa.selenium.By;

public final class PatientRegistrationPage extends BasePage {
    private static final By GIVEN_NAME = By.name("givenName");
    private static final By FAMILY_NAME = By.name("familyName");
    private static final By NEXT = By.id("next-button");
    private static final By GENDER = By.id("gender-field");
    private static final By BIRTH_DAY = By.id("birthdateDay-field");
    private static final By BIRTH_MONTH = By.id("birthdateMonth-field");
    private static final By BIRTH_YEAR = By.id("birthdateYear-field");
    private static final By ADDRESS = By.cssSelector("input[name='address1']");
    private static final By CITY = By.cssSelector("input[name='cityVillage']");
    private static final By COUNTRY = By.cssSelector("input[name='country']");
    private static final By POSTAL_CODE = By.cssSelector("input[name='postalCode']");
    private static final By PHONE = By.cssSelector("input[name='phoneNumber']");
    private static final By CONFIRM = By.id("submit");

    public PatientRegistrationPage(DriverSession driverSession, TestConfig config) {
        super(driverSession, config);
    }

    public String register(PatientProfile patient) {
        type(GIVEN_NAME, patient.givenName());
        type(FAMILY_NAME, patient.familyName());
        click(NEXT);

        select(GENDER, patient.genderLabel());
        click(NEXT);

        type(BIRTH_DAY, String.valueOf(patient.birthDate().getDayOfMonth()));
        select(
                BIRTH_MONTH,
                patient.birthDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        type(BIRTH_YEAR, String.valueOf(patient.birthDate().getYear()));
        click(NEXT);

        type(ADDRESS, patient.address());
        type(CITY, patient.city());
        type(COUNTRY, patient.country());
        type(POSTAL_CODE, patient.postalCode());
        click(NEXT);

        visible(PHONE);
        click(NEXT);

        click(NEXT);
        click(CONFIRM);
        waitForUrlToContain("patient.page?patientId=");

        return queryParameter(currentUrl(), "patientId");
    }

    private static String queryParameter(String url, String parameterName) {
        String query = URI.create(url).getRawQuery();
        if (query == null) {
            throw new IllegalStateException("Patient dashboard URL has no query string: " + url);
        }

        return Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair.length == 2 && parameterName.equals(pair[0]))
                .map(pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Patient dashboard URL has no '%s' parameter: %s"
                                .formatted(parameterName, url)));
    }
}
