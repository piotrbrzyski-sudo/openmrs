package org.openmrs.automation.api;

import static io.restassured.RestAssured.given;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openmrs.automation.config.TestConfig;
import org.openmrs.automation.model.ApiError;
import org.openmrs.automation.model.PatientProfile;
import org.openmrs.automation.model.PatientRecord;

public final class OpenMrsApiClient {
    private static final String PATIENT_REPRESENTATION =
            "custom:(uuid,identifiers:(identifier,preferred),"
                    + "person:(uuid,gender,birthdate,"
                    + "names:(uuid,givenName,familyName,preferred),"
                    + "addresses:(address1,cityVillage,country,postalCode,preferred)))";

    private final RequestSpecification requestSpecification;
    private final TestConfig config;

    public OpenMrsApiClient(TestConfig config) {
        this.config = config;
        this.requestSpecification = new RequestSpecBuilder()
                .setBaseUri(config.apiBaseUrl())
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .build();
    }

    public PatientRecord createPatient(PatientProfile patient) {
        IdentifierMetadata identifierMetadata = identifierMetadata();
        String identifier = generateIdentifier(identifierMetadata.sourceUuid());
        String locationUuid = locationUuid(config.loginLocation());

        Map<String, Object> person = new LinkedHashMap<>();
        person.put("gender", patient.gender());
        person.put("birthdate", patient.birthDate().toString());
        person.put("birthdateEstimated", false);
        person.put("dead", false);
        person.put("names", List.of(Map.of(
                "givenName", patient.givenName(),
                "familyName", patient.familyName(),
                "preferred", true)));
        person.put("addresses", List.of(Map.of(
                "address1", patient.address(),
                "cityVillage", patient.city(),
                "country", patient.country(),
                "postalCode", patient.postalCode(),
                "preferred", true)));

        Map<String, Object> identifierBody = new LinkedHashMap<>();
        identifierBody.put("identifier", identifier);
        identifierBody.put("identifierType", identifierMetadata.identifierTypeUuid());
        identifierBody.put("location", locationUuid);
        identifierBody.put("preferred", true);

        Response response = request()
                .body(Map.of(
                        "person", person,
                        "identifiers", List.of(identifierBody)))
                .post("/patient");

        requireStatus(response, 201, "create patient");
        String patientUuid = requiredJsonValue(response, "uuid", "created patient UUID");
        return getPatient(patientUuid);
    }

    public PatientRecord getPatient(String patientUuid) {
        Response response = request()
                .queryParam("v", PATIENT_REPRESENTATION)
                .get("/patient/{uuid}", patientUuid);

        requireStatus(response, 200, "retrieve patient " + patientUuid);
        return patientRecord(response);
    }

    public PatientRecord updateFamilyName(PatientRecord patient, String updatedFamilyName) {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("givenName", patient.givenName());
        update.put("familyName", updatedFamilyName);
        update.put("preferred", true);

        Response response = request()
                .body(update)
                .post(
                        "/person/{personUuid}/name/{nameUuid}",
                        patient.personUuid(),
                        patient.preferredNameUuid());

        requireStatus(response, 200, "update patient name");
        return getPatient(patient.patientUuid());
    }

    public ApiError createPatientWithInvalidData() {
        Map<String, Object> invalidPerson = new LinkedHashMap<>();
        invalidPerson.put("gender", "NOT_A_GENDER");
        invalidPerson.put(
                "names",
                List.of(Map.of(
                        "givenName", "Invalid",
                        "familyName", "Patient",
                        "preferred", true)));

        Response response = request()
                .body(Map.of(
                        "person", invalidPerson,
                        "identifiers", List.of()))
                .post("/patient");

        return apiError(response, "create patient with invalid data");
    }

    public ApiError retrievePatient(String patientUuid) {
        Response response = request()
                .queryParam("v", PATIENT_REPRESENTATION)
                .get("/patient/{uuid}", patientUuid);

        return apiError(response, "retrieve patient " + patientUuid);
    }

    public ApiError updatePatient(String patientUuid) {
        Response response = request()
                .body(Map.of("person", Map.of("gender", "M")))
                .post("/patient/{uuid}", patientUuid);

        return apiError(response, "update patient " + patientUuid);
    }

    public void voidPatient(String patientUuid) {
        Response response = request()
                .queryParam("reason", "Automated kata scenario cleanup")
                .delete("/patient/{uuid}", patientUuid);

        requireStatusCode(response, "void patient " + patientUuid, 204);
    }

    private IdentifierMetadata identifierMetadata() {
        Response response = request()
                .queryParam("v", "full")
                .get("/idgen/identifiersource");

        requireStatus(response, 200, "list patient identifier sources");
        requireJson(response, "list patient identifier sources");

        List<Map<String, Object>> sources = response.jsonPath().getList("results");
        if (sources == null) {
            throw new IllegalStateException("OpenMRS returned no identifier source collection");
        }

        return sources.stream()
                .filter(this::isOpenMrsIdentifierSource)
                .map(source -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> identifierType =
                            (Map<String, Object>) source.get("identifierType");
                    return new IdentifierMetadata(
                            String.valueOf(source.get("uuid")),
                            String.valueOf(identifierType.get("uuid")));
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No OpenMRS ID generator is configured on the target environment"));
    }

    private boolean isOpenMrsIdentifierSource(Map<String, Object> source) {
        Object rawIdentifierType = source.get("identifierType");
        if (!(rawIdentifierType instanceof Map<?, ?> identifierType)) {
            return false;
        }

        String name = String.valueOf(identifierType.get("name"));
        String display = String.valueOf(identifierType.get("display"));
        return "OpenMRS ID".equalsIgnoreCase(name) || "OpenMRS ID".equalsIgnoreCase(display);
    }

    private String generateIdentifier(String sourceUuid) {
        Response response = request()
                .body(Map.of(
                        "generateIdentifiers", true,
                        "sourceUuid", sourceUuid,
                        "numberToGenerate", 1))
                .post("/idgen/identifiersource");

        requireStatus(response, "generate patient identifier", 200, 201);
        String identifier = response.jsonPath().getString("identifiers[0]");
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalStateException("OpenMRS did not return a generated patient identifier");
        }
        return identifier;
    }

    private String locationUuid(String locationName) {
        Response response = request()
                .queryParam("q", locationName)
                .queryParam("v", "custom:(uuid,name,display)")
                .get("/location");

        requireStatus(response, 200, "find location " + locationName);
        requireJson(response, "find location");

        List<Map<String, Object>> locations = response.jsonPath().getList("results");
        if (locations == null) {
            throw new IllegalStateException("OpenMRS returned no location collection");
        }

        return locations.stream()
                .filter(location -> locationName.equalsIgnoreCase(String.valueOf(location.get("name")))
                        || locationName.equalsIgnoreCase(String.valueOf(location.get("display"))))
                .map(location -> String.valueOf(location.get("uuid")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No OpenMRS location matched '" + locationName + "'"));
    }

    private PatientRecord patientRecord(Response response) {
        requireJson(response, "parse patient");

        String patientUuid = requiredJsonValue(response, "uuid", "patient UUID");
        String personUuid = requiredJsonValue(response, "person.uuid", "person UUID");
        String preferredNameUuid = response.jsonPath()
                .getString("person.names.find { it.preferred == true }.uuid");
        String identifier =
                response.jsonPath().getString("identifiers.find { it.preferred == true }.identifier");
        String givenName = response.jsonPath()
                .getString("person.names.find { it.preferred == true }.givenName");
        String familyName = response.jsonPath()
                .getString("person.names.find { it.preferred == true }.familyName");
        String gender = requiredJsonValue(response, "person.gender", "patient gender");
        LocalDate birthDate = parseBirthDate(
                requiredJsonValue(response, "person.birthdate", "patient birth date"));
        String address = requiredJsonValue(
                response,
                "person.addresses.find { it.preferred == true }.address1",
                "preferred patient address");
        String city = requiredJsonValue(
                response,
                "person.addresses.find { it.preferred == true }.cityVillage",
                "preferred patient city");
        String country = requiredJsonValue(
                response,
                "person.addresses.find { it.preferred == true }.country",
                "preferred patient country");
        String postalCode = requiredJsonValue(
                response,
                "person.addresses.find { it.preferred == true }.postalCode",
                "preferred patient postal code");

        if (preferredNameUuid == null || identifier == null || givenName == null || familyName == null) {
            throw new IllegalStateException(
                    "Patient representation is missing a preferred name or identifier");
        }

        return new PatientRecord(
                patientUuid,
                personUuid,
                preferredNameUuid,
                identifier,
                givenName,
                familyName,
                gender,
                birthDate,
                address,
                city,
                country,
                postalCode);
    }

    private static ApiError apiError(Response response, String operation) {
        if (response.statusCode() < 400) {
            throw new AssertionError(
                    "Unable to %s: expected the request to be rejected but received HTTP %d. Response: %s"
                            .formatted(
                                    operation,
                                    response.statusCode(),
                                    abbreviated(response.asString())));
        }

        requireJson(response, operation);
        String message = response.jsonPath().getString("error.message");
        if (message == null || message.isBlank()) {
            throw new AssertionError(
                    "Unable to %s: JSON error is missing error.message. Response: %s"
                            .formatted(operation, abbreviated(response.asString())));
        }
        return new ApiError(response.statusCode(), message, true);
    }

    private RequestSpecification request() {
        return given()
                .spec(requestSpecification)
                .auth()
                .preemptive()
                .basic(config.username(), config.password());
    }

    private static String requiredJsonValue(
            Response response, String path, String description) {
        requireJson(response, "read " + description);
        String value = response.jsonPath().getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("OpenMRS response is missing " + description);
        }
        return value;
    }

    private static void requireStatus(Response response, int expected, String operation) {
        requireStatus(response, operation, expected);
    }

    private static void requireStatus(Response response, String operation, int... expectedStatuses) {
        requireStatusCode(response, operation, expectedStatuses);
        requireJson(response, operation);
    }

    private static void requireStatusCode(
            Response response, String operation, int... expectedStatuses) {
        boolean expected = Arrays.stream(expectedStatuses)
                .anyMatch(status -> status == response.statusCode());
        if (!expected) {
            String expectedDescription = Arrays.stream(expectedStatuses)
                    .mapToObj(String::valueOf)
                    .reduce((left, right) -> left + " or " + right)
                    .orElseThrow();
            throw new AssertionError(
                    "Unable to %s: expected HTTP %s but received %d. Response: %s"
                            .formatted(
                                    operation,
                                    expectedDescription,
                                    response.statusCode(),
                                    abbreviated(response.asString())));
        }
    }

    private static void requireJson(Response response, String operation) {
        String contentType = response.contentType();
        if (contentType == null || !contentType.toLowerCase().contains("json")) {
            throw new AssertionError(
                    ("Unable to %s: expected JSON but received '%s'. "
                                    + "The target may be unavailable or presenting an upstream browser challenge. "
                                    + "Response: %s")
                            .formatted(operation, contentType, abbreviated(response.asString())));
        }
    }

    private static String abbreviated(String body) {
        if (body == null) {
            return "<empty>";
        }
        String singleLine = body.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 500 ? singleLine : singleLine.substring(0, 500) + "...";
    }

    private static LocalDate parseBirthDate(String value) {
        if (value.length() < 10) {
            throw new IllegalStateException("OpenMRS returned an invalid patient birth date: " + value);
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private record IdentifierMetadata(String sourceUuid, String identifierTypeUuid) {
    }
}
