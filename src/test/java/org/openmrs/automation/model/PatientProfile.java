package org.openmrs.automation.model;

import java.time.LocalDate;

public record PatientProfile(
        String givenName,
        String familyName,
        String gender,
        LocalDate birthDate,
        String address,
        String city,
        String country,
        String postalCode) {

    public String fullName() {
        return givenName + " " + familyName;
    }

    public String genderLabel() {
        return switch (gender) {
            case "M" -> "Male";
            case "F" -> "Female";
            default -> throw new IllegalArgumentException("Unsupported OpenMRS gender: " + gender);
        };
    }

    public PatientProfile withFamilyName(String updatedFamilyName) {
        return new PatientProfile(
                givenName,
                updatedFamilyName,
                gender,
                birthDate,
                address,
                city,
                country,
                postalCode);
    }
}
