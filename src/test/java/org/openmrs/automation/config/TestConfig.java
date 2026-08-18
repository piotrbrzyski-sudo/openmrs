package org.openmrs.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

public final class TestConfig {
    private static final String CONFIG_RESOURCE = "config/application.properties";

    private final Properties properties = new Properties();

    public TestConfig() {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing test configuration: " + CONFIG_RESOURCE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + CONFIG_RESOURCE, exception);
        }
    }

    public String uiBaseUrl() {
        return withoutTrailingSlash(value("ui.base-url", "OPENMRS_UI_BASE_URL"));
    }

    public String apiBaseUrl() {
        return withoutTrailingSlash(value("api.base-url", "OPENMRS_API_BASE_URL"));
    }

    public String username() {
        return value("username", "OPENMRS_USERNAME");
    }

    public String password() {
        return value("password", "OPENMRS_PASSWORD");
    }

    public String loginLocation() {
        return value("login.location", "OPENMRS_LOGIN_LOCATION");
    }

    public String visitType() {
        return value("visit.type", "OPENMRS_VISIT_TYPE");
    }

    public String browser() {
        return value("browser", "BROWSER").toLowerCase(Locale.ROOT);
    }

    public boolean headless() {
        return Boolean.parseBoolean(value("headless", "HEADLESS"));
    }

    public boolean cleanUpCreatedPatients() {
        return Boolean.parseBoolean(value("cleanup.created-patients", "CLEANUP_CREATED_PATIENTS"));
    }

    public Duration uiTimeout() {
        return Duration.ofSeconds(Long.parseLong(value("timeout.seconds", "WAIT_TIMEOUT_SECONDS")));
    }

    private String value(String propertyName, String environmentName) {
        String systemValue = System.getProperty(propertyName);
        if (isPresent(systemValue)) {
            return systemValue.trim();
        }

        String environmentValue = System.getenv(environmentName);
        if (isPresent(environmentValue)) {
            return environmentValue.trim();
        }

        String fileValue = properties.getProperty(propertyName);
        if (isPresent(fileValue)) {
            return fileValue.trim();
        }

        throw new IllegalStateException(
                "Configuration value '%s' is missing (environment override: %s)"
                        .formatted(propertyName, environmentName));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String withoutTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }
}
