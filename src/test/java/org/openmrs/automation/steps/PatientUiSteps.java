package org.openmrs.automation.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openmrs.automation.context.ScenarioContext;
import org.openmrs.automation.pages.HomePage;
import org.openmrs.automation.pages.LoginPage;
import org.openmrs.automation.pages.PatientDashboardPage;
import org.openmrs.automation.pages.PatientRegistrationPage;
import org.openmrs.automation.pages.PatientSearchPage;
import org.testng.Assert;

public final class PatientUiSteps {
    private final ScenarioContext context;
    private final LoginPage loginPage;
    private final HomePage homePage;
    private final PatientSearchPage patientSearchPage;
    private final PatientRegistrationPage registrationPage;
    private final PatientDashboardPage dashboardPage;

    public PatientUiSteps(
            ScenarioContext context,
            LoginPage loginPage,
            HomePage homePage,
            PatientSearchPage patientSearchPage,
            PatientRegistrationPage registrationPage,
            PatientDashboardPage dashboardPage) {
        this.context = context;
        this.loginPage = loginPage;
        this.homePage = homePage;
        this.patientSearchPage = patientSearchPage;
        this.registrationPage = registrationPage;
        this.dashboardPage = dashboardPage;
    }

    @Given("a registrar is signed in to OpenMRS")
    public void signIn() {
        loginPage.login();
    }

    @When("the registrar finds the patient by their identifier")
    public void findPatientByIdentifier() {
        homePage.openPatientSearch();
        patientSearchPage.searchAndOpen(context.patientRecord().identifier());
    }

    @Then("the registrar sees demographics matching the clinical record")
    public void verifyClinicalDemographicsInUi() {
        Assert.assertEquals(
                dashboardPage.givenName(),
                context.patientProfile().givenName(),
                "UI given name differs from the API-created record");
        Assert.assertEquals(
                dashboardPage.familyName(),
                context.patientProfile().familyName(),
                "UI family name differs from the API-created record");
        Assert.assertEquals(
                dashboardPage.gender(),
                context.patientProfile().genderLabel(),
                "UI gender differs from the API-created record");
        Assert.assertEquals(
                dashboardPage.birthDate(),
                context.patientProfile().birthDate(),
                "UI birth date differs from the API-created record");
        Assert.assertTrue(
                dashboardPage.identifiers().contains(context.patientRecord().identifier()),
                "UI does not show the API-created patient identifier");
    }

    @When("the registrar registers the patient")
    public void registerPatient() {
        homePage.openPatientRegistration();
        context.patientUuid(registrationPage.register(context.patientProfile()));
    }

    @Given("the registrar opens the patient record")
    public void openPatientRecord() {
        dashboardPage.open(context.patientUuid());
    }

    @When("the registrar reopens the patient record")
    public void reopenPatientRecord() {
        openPatientRecord();
        dashboardPage.waitForFamilyNameToPropagate(context.correctedFamilyName());
    }

    @Then("the corrected family name is visible to the registrar")
    public void verifyCorrectedFamilyName() {
        Assert.assertEquals(
                dashboardPage.familyName(),
                context.correctedFamilyName(),
                "UI did not display the family-name correction made through the API");
    }

    @When("the registrar starts and closes a facility visit")
    public void completeVisitLifecycle() {
        dashboardPage.startAndCloseVisit();
    }

    @Then("the patient is shown without an active visit")
    public void verifyVisitIsClosed() {
        dashboardPage.open(context.patientUuid());
        Assert.assertFalse(
                dashboardPage.hasActiveVisit(),
                "Patient dashboard still indicates an active visit");
        Assert.assertTrue(
                dashboardPage.canStartVisit(),
                "Patient dashboard does not offer the action to start a new visit");
    }
}
