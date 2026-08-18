@api @negative-validation
Feature: API negative validation
  OpenMRS must reject invalid patient requests with its JSON error contract.

  Scenario: Create patient with invalid data
    When a patient is created with invalid data through OpenMRS clinical services
    Then OpenMRS clinical services return HTTP 400 with a JSON error

  Scenario: Retrieve non-existing patient
    When a non-existing patient is retrieved through OpenMRS clinical services
    Then OpenMRS clinical services return HTTP 404 with a JSON error

  Scenario: Update non-existing patient
    When a non-existing patient is updated through OpenMRS clinical services
    Then OpenMRS clinical services return HTTP 404 with a JSON error
