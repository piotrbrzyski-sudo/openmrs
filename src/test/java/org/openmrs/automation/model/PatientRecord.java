package org.openmrs.automation.model;

import java.time.LocalDate;

public record PatientRecord(
        String patientUuid,
        String personUuid,
        String preferredNameUuid,
        String identifier,
        String givenName,
        String familyName,
        String gender,
        LocalDate birthDate,
        String address,
        String city,
        String country,
        String postalCode) {
}
