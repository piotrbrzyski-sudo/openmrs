package org.openmrs.automation.context;

import org.openmrs.automation.model.ApiError;
import org.openmrs.automation.model.PatientProfile;
import org.openmrs.automation.model.PatientRecord;

public final class ScenarioContext {
    private PatientProfile patientProfile;
    private PatientRecord patientRecord;
    private String patientUuid;
    private String correctedFamilyName;
    private ApiError apiError;

    public PatientProfile patientProfile() {
        return require(patientProfile, "patient profile");
    }

    public void patientProfile(PatientProfile patientProfile) {
        this.patientProfile = patientProfile;
    }

    public PatientRecord patientRecord() {
        return require(patientRecord, "patient record");
    }

    public void patientRecord(PatientRecord patientRecord) {
        this.patientRecord = patientRecord;
        this.patientUuid = patientRecord.patientUuid();
    }

    public String patientUuid() {
        return require(patientUuid, "patient UUID");
    }

    public void patientUuid(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    public boolean hasPatientUuid() {
        return patientUuid != null && !patientUuid.isBlank();
    }

    public String correctedFamilyName() {
        return require(correctedFamilyName, "corrected family name");
    }

    public void correctedFamilyName(String correctedFamilyName) {
        this.correctedFamilyName = correctedFamilyName;
    }

    public ApiError apiError() {
        return require(apiError, "API error");
    }

    public void apiError(ApiError apiError) {
        this.apiError = apiError;
    }

    private static <T> T require(T value, String description) {
        if (value == null) {
            throw new IllegalStateException("Scenario does not yet contain a " + description);
        }
        return value;
    }
}
