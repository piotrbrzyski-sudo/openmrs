package org.openmrs.automation.model;

public record ApiError(int statusCode, String message, boolean json) {
    public ApiError {
        if (statusCode < 400) {
            throw new IllegalArgumentException("API error status must be at least 400");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("API error message must not be blank");
        }
        if (!json) {
            throw new IllegalArgumentException("API error must use the OpenMRS JSON error contract");
        }
    }
}
