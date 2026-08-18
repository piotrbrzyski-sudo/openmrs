package org.openmrs.automation.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openmrs.automation.api.OpenMrsApiClient;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.context.ScenarioContext;
import org.openmrs.automation.driver.DriverSession;

public final class ScenarioHooks {
    private final ScenarioContext context;
    private final DriverSession driverSession;
    private final OpenMrsApiClient apiClient;
    private final TestConfig config;

    public ScenarioHooks(
            ScenarioContext context,
            DriverSession driverSession,
            OpenMrsApiClient apiClient,
            TestConfig config) {
        this.context = context;
        this.driverSession = driverSession;
        this.apiClient = apiClient;
        this.config = config;
    }

    @After
    public void afterScenario(Scenario scenario) {
        attachFailureScreenshot(scenario);
        driverSession.quit();
        cleanUpPatient(scenario);
    }

    private void attachFailureScreenshot(Scenario scenario) {
        if (!scenario.isFailed() || !driverSession.isStarted()) {
            return;
        }

        try {
            byte[] screenshot = driverSession.screenshot();
            if (screenshot.length > 0) {
                scenario.attach(screenshot, "image/png", "Failure screenshot");
            }
        } catch (RuntimeException exception) {
            scenario.log("Could not capture failure screenshot: " + exception.getMessage());
        }
    }

    private void cleanUpPatient(Scenario scenario) {
        if (!config.cleanUpCreatedPatients() || !context.hasPatientUuid()) {
            return;
        }

        try {
            apiClient.voidPatient(context.patientUuid());
        } catch (RuntimeException | AssertionError exception) {
            scenario.log("Could not void generated patient during cleanup: " + exception.getMessage());
        }
    }
}
