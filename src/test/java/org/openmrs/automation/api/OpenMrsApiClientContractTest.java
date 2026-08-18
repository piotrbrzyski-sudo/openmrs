package org.openmrs.automation.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.model.ApiError;
import org.openmrs.automation.model.PatientProfile;
import org.openmrs.automation.model.PatientRecord;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public final class OpenMrsApiClientContractTest {
    private static final String API_PATH = "/ws/rest/v1";
    private static final String USERNAME = "contract-user";
    private static final String PASSWORD = "contract-password";
    private static final String LOGIN_LOCATION = "Inpatient Ward";
    private static final String PATIENT_UUID = "patient-uuid";
    private static final String PERSON_UUID = "person-uuid";
    private static final String NAME_UUID = "name-uuid";
    private static final String IDENTIFIER = "1000A";
    private static final String MISSING_PATIENT_UUID = "00000000-0000-0000-0000-000000000000";

    private static final String[] OVERRIDDEN_PROPERTIES = {
        "api.base-url", "username", "password", "login.location"
    };

    private final Map<String, String> previousProperties = new HashMap<>();
    private WireMockServer wireMock;

    @BeforeClass
    public void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @BeforeMethod
    public void configureClientTarget() {
        previousProperties.clear();
        for (String property : OVERRIDDEN_PROPERTIES) {
            previousProperties.put(property, System.getProperty(property));
        }

        System.setProperty("api.base-url", wireMock.baseUrl() + API_PATH);
        System.setProperty("username", USERNAME);
        System.setProperty("password", PASSWORD);
        System.setProperty("login.location", LOGIN_LOCATION);
        wireMock.resetAll();
    }

    @AfterMethod(alwaysRun = true)
    public void restoreClientTarget() {
        for (String property : OVERRIDDEN_PROPERTIES) {
            String previousValue = previousProperties.get(property);
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    @Test
    public void createsPatientUsingDiscoveredTargetMetadata() {
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/idgen/identifiersource"))
                .willReturn(json(200, """
                        {
                          "results": [{
                            "uuid": "source-uuid",
                            "name": "OpenMRS ID Generator",
                            "identifierType": {
                              "uuid": "identifier-type-uuid",
                              "name": "OpenMRS ID",
                              "display": "OpenMRS ID"
                            }
                          }]
                        }
                        """)));
        wireMock.stubFor(post(urlPathEqualTo(API_PATH + "/idgen/identifiersource"))
                .willReturn(json(201, """
                        {"identifiers": ["1000A"]}
                        """)));
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/location"))
                .willReturn(json(200, """
                        {"results": [{"uuid": "location-uuid", "name": "Inpatient Ward"}]}
                        """)));
        wireMock.stubFor(post(urlPathEqualTo(API_PATH + "/patient"))
                .willReturn(json(201, """
                        {"uuid": "patient-uuid"}
                        """)));
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(json(200, patientJson("Jones"))));

        PatientProfile profile = new PatientProfile(
                "Alice",
                "Jones",
                "F",
                LocalDate.of(1990, 4, 12),
                "1 Test Street",
                "Testville",
                "Poland",
                "00-001");

        PatientRecord patient = client().createPatient(profile);

        assertEquals(patient.patientUuid(), PATIENT_UUID);
        assertEquals(patient.personUuid(), PERSON_UUID);
        assertEquals(patient.preferredNameUuid(), NAME_UUID);
        assertEquals(patient.identifier(), IDENTIFIER);
        assertEquals(patient.givenName(), "Alice");
        assertEquals(patient.familyName(), "Jones");
        assertEquals(patient.gender(), "F");
        assertEquals(patient.birthDate(), LocalDate.of(1990, 4, 12));
        assertEquals(patient.address(), "1 Test Street");
        assertEquals(patient.city(), "Testville");
        assertEquals(patient.country(), "Poland");
        assertEquals(patient.postalCode(), "00-001");

        wireMock.verify(getRequestedFor(urlPathEqualTo(API_PATH + "/idgen/identifiersource"))
                .withQueryParam("v", equalTo("full")));
        wireMock.verify(postRequestedFor(urlPathEqualTo(API_PATH + "/idgen/identifiersource"))
                .withRequestBody(matchingJsonPath("$.generateIdentifiers", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.sourceUuid", equalTo("source-uuid")))
                .withRequestBody(matchingJsonPath("$.numberToGenerate", equalTo("1"))));
        wireMock.verify(getRequestedFor(urlPathEqualTo(API_PATH + "/location"))
                .withQueryParam("q", equalTo(LOGIN_LOCATION)));
        wireMock.verify(postRequestedFor(urlPathEqualTo(API_PATH + "/patient"))
                .withHeader("Authorization", equalTo(basicAuthorization()))
                .withRequestBody(matchingJsonPath("$.person.gender", equalTo("F")))
                .withRequestBody(matchingJsonPath("$.person.birthdate", equalTo("1990-04-12")))
                .withRequestBody(matchingJsonPath("$.person.names[0].givenName", equalTo("Alice")))
                .withRequestBody(matchingJsonPath("$.person.names[0].familyName", equalTo("Jones")))
                .withRequestBody(
                        matchingJsonPath("$.person.addresses[0].address1", equalTo("1 Test Street")))
                .withRequestBody(
                        matchingJsonPath("$.person.addresses[0].cityVillage", equalTo("Testville")))
                .withRequestBody(
                        matchingJsonPath("$.identifiers[0].identifierType", equalTo("identifier-type-uuid")))
                .withRequestBody(matchingJsonPath("$.identifiers[0].location", equalTo("location-uuid"))));
    }

    @Test
    public void updatesAndVoidsPatientUsingTheOpenMrsContract() {
        wireMock.stubFor(post(urlPathEqualTo(API_PATH + "/person/" + PERSON_UUID + "/name/" + NAME_UUID))
                .willReturn(json(200, "{}")));
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(json(200, patientJson("Nowak"))));
        wireMock.stubFor(delete(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(aResponse().withStatus(204)));

        PatientRecord existing =
                new PatientRecord(
                        PATIENT_UUID,
                        PERSON_UUID,
                        NAME_UUID,
                        IDENTIFIER,
                        "Alice",
                        "Jones",
                        "F",
                        LocalDate.of(1990, 4, 12),
                        "1 Test Street",
                        "Testville",
                        "Poland",
                        "00-001");
        OpenMrsApiClient client = client();

        PatientRecord updated = client.updateFamilyName(existing, "Nowak");
        client.voidPatient(PATIENT_UUID);

        assertEquals(updated.familyName(), "Nowak");
        wireMock.verify(postRequestedFor(
                        urlPathEqualTo(API_PATH + "/person/" + PERSON_UUID + "/name/" + NAME_UUID))
                .withRequestBody(matchingJsonPath("$.givenName", equalTo("Alice")))
                .withRequestBody(matchingJsonPath("$.familyName", equalTo("Nowak")))
                .withRequestBody(matchingJsonPath("$.preferred", equalTo("true"))));
        wireMock.verify(deleteRequestedFor(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .withQueryParam("reason", equalTo("Automated kata scenario cleanup")));
    }

    @Test
    public void reportsFailedPatientCleanupWithStatusAndResponseBody() {
        wireMock.stubFor(delete(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(json(403, """
                        {"error": {"message": "Cleanup permission denied"}}
                        """)));

        AssertionError error =
                expectThrows(AssertionError.class, () -> client().voidPatient(PATIENT_UUID));

        assertTrue(error.getMessage().contains("expected HTTP 204 but received 403"));
        assertTrue(error.getMessage().contains("Cleanup permission denied"));
    }

    @Test
    public void createsPatientWithInvalidDataAndExposesErrorResponse() {
        wireMock.stubFor(post(urlPathEqualTo(API_PATH + "/patient"))
                .willReturn(json(400, """
                        {"error": {"message": "Invalid gender"}}
                        """)));

        ApiError error = client().createPatientWithInvalidData();

        assertEquals(error.statusCode(), 400);
        assertEquals(error.message(), "Invalid gender");
        assertTrue(error.json());
        wireMock.verify(postRequestedFor(urlPathEqualTo(API_PATH + "/patient"))
                .withRequestBody(matchingJsonPath("$.person.gender", equalTo("NOT_A_GENDER")))
                .withRequestBody(matchingJsonPath("$.person.names[0].givenName", equalTo("Invalid")))
                .withRequestBody(matchingJsonPath("$.identifiers.size()", equalTo("0"))));
    }

    @Test
    public void retrievesNonExistingPatientAndExposesErrorResponse() {
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + MISSING_PATIENT_UUID))
                .willReturn(json(404, """
                        {"error": {"message": "Patient not found"}}
                        """)));

        ApiError error = client().retrievePatient(MISSING_PATIENT_UUID);

        assertEquals(error.statusCode(), 404);
        assertEquals(error.message(), "Patient not found");
        assertTrue(error.json());
        wireMock.verify(getRequestedFor(
                        urlPathEqualTo(API_PATH + "/patient/" + MISSING_PATIENT_UUID))
                .withHeader("Authorization", equalTo(basicAuthorization())));
    }

    @Test
    public void updatesNonExistingPatientAndExposesErrorResponse() {
        wireMock.stubFor(post(urlPathEqualTo(API_PATH + "/patient/" + MISSING_PATIENT_UUID))
                .willReturn(json(404, """
                        {"error": {"message": "Patient not found"}}
                        """)));

        ApiError error = client().updatePatient(MISSING_PATIENT_UUID);

        assertEquals(error.statusCode(), 404);
        assertEquals(error.message(), "Patient not found");
        assertTrue(error.json());
        wireMock.verify(postRequestedFor(
                        urlPathEqualTo(API_PATH + "/patient/" + MISSING_PATIENT_UUID))
                .withHeader("Authorization", equalTo(basicAuthorization()))
                .withRequestBody(matchingJsonPath("$.person.gender", equalTo("M"))));
    }

    @Test
    public void rejectsHtmlErrorInsteadOfTreatingItAsTheJsonContract() {
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + MISSING_PATIENT_UUID))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/html;charset=utf-8")
                        .withBody("""
                                <!doctype html>
                                <html><head><title>HTTP Status 500 - Internal Server Error</title></head></html>
                                """)));

        AssertionError error =
                expectThrows(AssertionError.class, () -> client().retrievePatient(MISSING_PATIENT_UUID));

        assertTrue(error.getMessage().contains("expected JSON"));
        assertTrue(error.getMessage().contains("HTTP Status 500"));
    }

    @Test
    public void reportsUnexpectedStatusWithTheOpenMrsResponseBody() {
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(json(503, """
                        {"error": {"message": "maintenance"}}
                        """)));

        AssertionError error =
                expectThrows(AssertionError.class, () -> client().getPatient(PATIENT_UUID));

        assertTrue(error.getMessage().contains("expected HTTP 200 but received 503"));
        assertTrue(error.getMessage().contains("maintenance"));
    }

    @Test
    public void identifiesAnUpstreamHtmlChallengeInsteadOfParsingItAsJson() {
        wireMock.stubFor(get(urlPathEqualTo(API_PATH + "/patient/" + PATIENT_UUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><title>Checking your browser</title></html>")));

        AssertionError error =
                expectThrows(AssertionError.class, () -> client().getPatient(PATIENT_UUID));

        assertTrue(error.getMessage().contains("expected JSON"));
        assertTrue(error.getMessage().contains("upstream browser challenge"));
    }

    private OpenMrsApiClient client() {
        return new OpenMrsApiClient(new TestConfig());
    }

    private static ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String patientJson(String familyName) {
        return """
                {
                  "uuid": "patient-uuid",
                  "identifiers": [{"identifier": "1000A", "preferred": true}],
                  "person": {
                    "uuid": "person-uuid",
                    "gender": "F",
                    "birthdate": "1990-04-12T00:00:00.000+0000",
                    "names": [{
                      "uuid": "name-uuid",
                      "givenName": "Alice",
                      "familyName": "%s",
                      "preferred": true
                    }],
                    "addresses": [{
                      "address1": "1 Test Street",
                      "cityVillage": "Testville",
                      "country": "Poland",
                      "postalCode": "00-001",
                      "preferred": true
                    }]
                  }
                }
                """
                .formatted(familyName);
    }

    private static String basicAuthorization() {
        String credentials = USERNAME + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
