@hybrid
Feature: Keep patient care records consistent across OpenMRS channels
  Clinical and registration teams need the same trustworthy patient information,
  regardless of whether it was established through clinical services or the web application.

  @create @retrieve @patient-search
  Scenario: A patient created through clinical services is available to a registrar
    Given a unique patient profile is prepared for clinical intake
    When the patient is created through OpenMRS clinical services
    Then clinical services return the same patient record
    When a registrar is signed in to OpenMRS
    And the registrar finds the patient by their identifier
    Then the registrar sees demographics matching the clinical record

  @registration @retrieve
  Scenario: A patient registered by a registrar is available through clinical services
    Given a registrar is signed in to OpenMRS
    And a unique patient profile is prepared for registration
    When the registrar registers the patient
    Then clinical services can retrieve the registered patient

  @update
  Scenario: A family name corrected through clinical services is visible to a registrar
    Given a unique patient profile is prepared for clinical intake
    And the patient is created through OpenMRS clinical services
    When the patient's family name is corrected through OpenMRS clinical services
    And a registrar is signed in to OpenMRS
    And the registrar reopens the patient record
    Then the corrected family name is visible to the registrar

  @visit
  Scenario: A registrar can start and close a facility visit
    Given a unique patient profile is prepared for clinical intake
    And the patient is created through OpenMRS clinical services
    And a registrar is signed in to OpenMRS
    And the registrar opens the patient record
    When the registrar starts and closes a facility visit
    Then the patient is shown without an active visit
