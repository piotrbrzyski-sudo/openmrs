package org.openmrs.automation.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.openmrs.automation.api.OpenMrsApiClient;
import org.openmrs.automation.context.ScenarioContext;
import org.openmrs.automation.model.ApiError;
import org.openmrs.automation.model.PatientDataFactory;
import org.openmrs.automation.model.PatientProfile;
import org.openmrs.automation.model.PatientRecord;
import org.testng.Assert;

public final class PatientApiSteps {
    private final ScenarioContext context;
    private final OpenMrsApiClient apiClient;

    public PatientApiSteps(ScenarioContext context, OpenMrsApiClient apiClient) {
        this.context = context;
        this.apiClient = apiClient;
    }

    @Given("a unique patient profile is prepared for clinical intake")
    public void prepareClinicalPatient() {
        context.patientProfile(PatientDataFactory.uniquePatient("Api"));
    }

    @Given("a unique patient profile is prepared for registration")
    public void prepareRegistrationPatient() {
        context.patientProfile(PatientDataFactory.uniquePatient("Ui"));
    }

    @When("the patient is created through OpenMRS clinical services")
    public void createPatientThroughApi() {
        PatientRecord created = apiClient.createPatient(context.patientProfile());
        context.patientRecord(created);
    }

    @Then("clinical services return the same patient record")
    public void retrieveApiCreatedPatient() {
        PatientRecord retrieved = apiClient.getPatient(context.patientUuid());
        assertRecordMatches(retrieved, context.patientProfile());
        context.patientRecord(retrieved);
    }

    @Then("clinical services can retrieve the registered patient")
    public void retrieveUiCreatedPatient() {
        PatientRecord retrieved = apiClient.getPatient(context.patientUuid());
        assertRecordMatches(retrieved, context.patientProfile());
        context.patientRecord(retrieved);
    }

    @When("the patient's family name is corrected through OpenMRS clinical services")
    public void correctPatientFamilyName() {
        String correctedName = PatientDataFactory.correctedFamilyName();
        PatientRecord updated =
                apiClient.updateFamilyName(context.patientRecord(), correctedName);

        context.correctedFamilyName(correctedName);
        context.patientProfile(context.patientProfile().withFamilyName(correctedName));
        context.patientRecord(updated);
    }

    @When("a patient is created with invalid data through OpenMRS clinical services")
    public void createPatientWithInvalidData() {
        context.apiError(apiClient.createPatientWithInvalidData());
    }

    @When("a non-existing patient is retrieved through OpenMRS clinical services")
    public void retrieveNonExistingPatient() {
        context.apiError(apiClient.retrievePatient(UUID.randomUUID().toString()));
    }

    @When("a non-existing patient is updated through OpenMRS clinical services")
    public void updateNonExistingPatient() {
        context.apiError(apiClient.updatePatient(UUID.randomUUID().toString()));
    }

    @Then("OpenMRS clinical services return HTTP {int} with a JSON error")
    public void verifyJsonError(int expectedStatus) {
        ApiError error = context.apiError();
        Assert.assertEquals(
                error.statusCode(),
                expectedStatus,
                "OpenMRS returned an unexpected status");
        Assert.assertTrue(
                error.json(),
                "Error response should use the OpenMRS JSON error contract");
        Assert.assertFalse(
                error.message().isBlank(),
                "JSON error should include error.message");
    }

    private static void assertRecordMatches(PatientRecord actual, PatientProfile expected) {
        Assert.assertEquals(actual.givenName(), expected.givenName(), "Given name differs");
        Assert.assertEquals(actual.familyName(), expected.familyName(), "Family name differs");
        Assert.assertEquals(actual.gender(), expected.gender(), "Gender differs");
        Assert.assertEquals(actual.birthDate(), expected.birthDate(), "Birth date differs");
        Assert.assertEquals(actual.address(), expected.address(), "Address line differs");
        Assert.assertEquals(actual.city(), expected.city(), "City differs");
        Assert.assertEquals(actual.country(), expected.country(), "Country differs");
        Assert.assertEquals(actual.postalCode(), expected.postalCode(), "Postal code differs");
        Assert.assertFalse(actual.identifier().isBlank(), "Patient identifier should be assigned");
    }
}
