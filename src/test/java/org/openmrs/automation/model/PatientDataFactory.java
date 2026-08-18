package org.openmrs.automation.model;

import java.time.LocalDate;
import java.util.UUID;

public final class PatientDataFactory {
    private PatientDataFactory() {
    }

    public static PatientProfile uniquePatient(String source) {
        String token = lettersOnlyToken();
        return new PatientProfile(
                source + token,
                "Kata" + token,
                "F",
                LocalDate.of(1992, 6, 15),
                "42 Automation Avenue",
                "Testville",
                "Kenya",
                "00100");
    }

    public static String correctedFamilyName() {
        return "Verified" + lettersOnlyToken();
    }

    private static String lettersOnlyToken() {
        String hexadecimal = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        StringBuilder letters = new StringBuilder(hexadecimal.length());
        for (char character : hexadecimal.toCharArray()) {
            if (Character.isDigit(character)) {
                letters.append((char) ('A' + (character - '0')));
            } else {
                letters.append(Character.toUpperCase(character));
            }
        }
        return letters.toString();
    }
}
