# OpenMRS Hybrid Test Automation Kata

An executable acceptance-test project for the OpenMRS Reference Application 2.x
web interface and REST API.

The project uses API calls for fast and precise data setup, then verifies the
same state through the UI. It also covers the opposite direction: a patient
registered in the browser is retrieved through the API.

The default target is a disposable OpenMRS environment managed by Docker
Compose. The public OpenMRS demo is not used as a pull-request gate because its
availability and anti-bot policy are outside this project's control.

Public repository: https://github.com/piotrbrzyski-sudo/openmrs

## What is tested

The acceptance suite contains focused scenarios for:

1. creating a patient through the REST API and finding the patient in the UI;
2. registering a patient in the UI and retrieving the patient through the API;
3. updating a patient's preferred family name through the API and observing the
   change in the UI;
4. starting and closing a Facility Visit through the UI;
5. verifying that OpenMRS rejects invalid patient creation and attempts to
   retrieve or update a non-existing patient.

The separate WireMock suite verifies the HTTP client in isolation:

- runtime discovery of identifier sources and locations;
- patient request serialization and response parsing;
- preferred-name updates;
- patient cleanup;
- Basic authentication;
- unexpected status diagnostics;
- non-JSON upstream responses;
- negative create, retrieve and update patient requests.

WireMock tests the client contract, not the OpenMRS provider. The real negative
API validation is therefore also part of the Compose-backed acceptance suite.

## Technology

- Java 17
- Maven
- Cucumber JVM
- TestNG
- PicoContainer
- Selenium WebDriver and Selenium Manager
- REST Assured
- WireMock
- Docker Compose
- GitHub Actions

## Architecture

```text
.
├── .github/workflows
│   ├── acceptance-tests.yml       # Deterministic CI gate
│   └── public-demo-smoke.yml      # Optional, non-gating public smoke
├── docker-compose.yml             # Pinned RefApp 2.x and MySQL target
├── pom.xml
└── src/test
    ├── java/org/openmrs/automation
    │   ├── api                    # REST client and WireMock contracts
    │   ├── config                 # Properties, environment and system overrides
    │   ├── context                # Scenario-scoped shared state
    │   ├── driver                 # Lazy WebDriver lifecycle
    │   ├── hooks                  # Screenshots, browser shutdown and cleanup
    │   ├── model                  # Patient test data and response records
    │   ├── pages                  # Page Objects and UI locators
    │   ├── runner                 # TestNG Cucumber runner
    │   └── steps                  # Thin business-step coordinators
    └── resources
        ├── config/application.properties
        ├── features
        │   ├── patient_api_validation.feature
        │   └── patient_hybrid_management.feature
        ├── testng-acceptance.xml
        ├── testng-contract.xml
        └── testng.xml
```

The Gherkin layer describes business behavior. Step definitions coordinate Page
Objects or `OpenMrsApiClient`; they do not contain Selenium locators or raw HTTP
requests. PicoContainer creates scenario-scoped objects, so scenarios do not
share mutable static state.

`OpenMrsApiClient` discovers the identifier source, identifier type and login
location at runtime. Environment-specific OpenMRS UUIDs are not hardcoded in
features or step definitions.

## pom.xml structure

`pom.xml` defines a test-only Maven project. There is no production `src/main`
tree: every dependency is `test` scoped and Surefire is the only execution
plugin.

### Properties

Centralized versions live in `<properties>`:

- `maven.compiler.release` — Java 17
- `cucumber.version` — Cucumber JVM, TestNG bridge and PicoContainer
- `selenium.version` — Selenium WebDriver
- `rest-assured.version` — REST Assured API client
- `wiremock.version` — isolated HTTP contract tests
- `testng.version` — test runner
- `jackson.version` — JSON serialization used by REST Assured
- `slf4j.version` — test logging

### Dependencies

| Library | Role |
|---|---|
| `cucumber-java` | Gherkin step definitions |
| `cucumber-testng` | TestNG Cucumber runner |
| `cucumber-picocontainer` | Scenario-scoped constructor injection |
| `selenium-java` | UI automation and Page Objects |
| `rest-assured` | Encapsulated OpenMRS REST client |
| `wiremock` | Client-contract tests without a live server |
| `jackson-databind` | JSON request and response mapping |
| `testng` | Suite XML files and assertions |
| `slf4j-simple` | Quiet test logging |

### Build plugins

- `maven-compiler-plugin` compiles the test sources for Java 17 with
  `-parameters`, which PicoContainer needs for constructor injection.
- `maven-surefire-plugin` runs `src/test/resources/testng.xml` by default.
  That suite includes the WireMock contracts and the Cucumber acceptance
  runner. Individual suites can be selected with
  `-Dsurefire.suiteXmlFiles=src/test/resources/testng-contract.xml` or
  `testng-acceptance.xml`.

## Prerequisites

- JDK 17 or newer
- Maven 3.9 or newer
- Docker with Docker Compose v2
- Google Chrome or Mozilla Firefox

Selenium Manager resolves the matching browser driver automatically.

The pinned Reference Application 2.x image is `linux/amd64`. Docker Desktop can
run it on Apple Silicon through emulation, but the first initialization will be
slower than on an x86-64 host.

## Quick start

### 1. Run the fast client contract suite

This suite requires no browser, Docker or external OpenMRS server:

```bash
mvn --batch-mode --no-transfer-progress clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-contract.xml
```

### 2. Start the controlled OpenMRS environment

Start from clean volumes when you need a deterministic database:

```bash
docker compose down --volumes --remove-orphans
docker compose up --detach --wait --wait-timeout 1200
```

The first run creates the OpenMRS schema and demo data and can take several
minutes. Compose reports success only after an authenticated REST session is
available. Do not restart the OpenMRS container while Liquibase is running.

OpenMRS will be available at:

```text
http://localhost:8080/openmrs
```

Default credentials:

```text
Username: admin
Password: Admin123
Location: Inpatient Ward
```

### 3. Run the acceptance suite

```bash
HEADLESS=true mvn --batch-mode --no-transfer-progress clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-acceptance.xml
```

Omit `HEADLESS=true` to observe the browser locally.

### 4. Stop and reset the environment

```bash
docker compose down --volumes --remove-orphans
```

Removing volumes guarantees that the next startup uses a fresh database.

## Test execution

Run contract and acceptance suites together after OpenMRS is healthy:

```bash
HEADLESS=true mvn clean test
```

Run selected Cucumber tags:

```bash
HEADLESS=true mvn clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-acceptance.xml \
  -Dcucumber.filter.tags="@create"

HEADLESS=true mvn clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-acceptance.xml \
  -Dcucumber.filter.tags="@registration"

HEADLESS=true mvn clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-acceptance.xml \
  -Dcucumber.filter.tags="@negative-validation"
```

The acceptance scenarios run sequentially because they share one controlled
OpenMRS instance. Every generated patient is voided by an `@After` hook unless
cleanup is explicitly disabled.

## Configuration

Defaults live in:

```text
src/test/resources/config/application.properties
```

Values are resolved in this order:

1. Java system property;
2. environment variable;
3. properties file.

Available settings:

- `ui.base-url` / `OPENMRS_UI_BASE_URL`
- `api.base-url` / `OPENMRS_API_BASE_URL`
- `username` / `OPENMRS_USERNAME`
- `password` / `OPENMRS_PASSWORD`
- `login.location` / `OPENMRS_LOGIN_LOCATION`
- `visit.type` / `OPENMRS_VISIT_TYPE`
- `browser` / `BROWSER` (`chrome` or `firefox`)
- `headless` / `HEADLESS`
- `timeout.seconds` / `WAIT_TIMEOUT_SECONDS`
- `cleanup.created-patients` / `CLEANUP_CREATED_PATIENTS`

Example:

```bash
OPENMRS_UI_BASE_URL=http://localhost:8080/openmrs \
OPENMRS_API_BASE_URL=http://localhost:8080/openmrs/ws/rest/v1 \
OPENMRS_USERNAME=admin \
OPENMRS_PASSWORD=Admin123 \
HEADLESS=true \
mvn clean test \
  -Dsurefire.suiteXmlFiles=src/test/resources/testng-acceptance.xml
```

Do not commit credentials for private environments. Supply them through the CI
secret store or environment variables.

## Reporting, Observability, and CI/CD

### Test Reports

Every acceptance run creates:

- `target/cucumber-reports/cucumber.html`
- `target/cucumber-reports/cucumber.json`
- `target/cucumber-reports/cucumber.xml`
- `target/cucumber-reports/rerun.txt`
- `target/surefire-reports/`

Open the HTML report locally:

```bash
open target/cucumber-reports/cucumber.html
```

On Linux use `xdg-open`; on Windows use `Start-Process`:

```bash
xdg-open target/cucumber-reports/cucumber.html

Start-Process target/cucumber-reports/cucumber.html
```

### Continuous Integration

`.github/workflows/acceptance-tests.yml` runs on every push, pull request and
manual dispatch.

It performs two jobs:

1. `contract-tests` runs the WireMock suite without Docker;
2. `acceptance-tests` starts the pinned Compose environment, waits for the
   authenticated REST healthcheck, runs headless Chrome scenarios, captures
   service logs and removes containers and volumes.

The acceptance job runs only after the contract suite succeeds. Reports are
uploaded even when a test or infrastructure step fails:

- contract job: Surefire reports;
- acceptance job: Cucumber reports, Surefire reports and Compose logs.

Artifacts are retained for 14 days. This repository does not build a deployable
application, so artifact publication is its delivery stage.

Concurrent runs for the same branch are cancelled to avoid wasting runner
capacity.

### Observability and stability

The UI layer uses:

- zero implicit wait;
- explicit waits for visibility, editability and clickability;
- URL and document-readiness checks;
- jQuery network-idle detection where jQuery is available;
- bounded polling with repeated page refreshes for eventual API-to-UI
  propagation;
- unique patient data to avoid collisions.

There are no unconditional sleeps in business scenarios or Page Objects.

The browser is created lazily and always closed by a Cucumber hook. Failed UI
scenarios include a PNG screenshot. API failures report the expected and actual
HTTP status and an abbreviated response body. Non-JSON errors from legacy
OpenMRS handlers, proxies or browser challenges are identified explicitly
instead of being parsed as JSON.

Patient cleanup validates the server's `204` response. Cleanup failures are
logged without hiding the original scenario failure.

## Public demo smoke test

`.github/workflows/public-demo-smoke.yml` runs only:

- on manual dispatch;
- on its weekly schedule.

It targets `https://demo.openmrs.org` and is deliberately excluded from
pull-request gating. The public service can change data, modules, credentials,
rate limits and edge-security rules without notice.

Cloudflare may replace the OpenMRS UI or API with a Turnstile challenge. The
suite detects this state and reports it, but does not attempt to bypass the
challenge, reuse browser cookies or disguise automation.

## Troubleshooting

### Compose does not become healthy

Inspect container state and logs:

```bash
docker compose ps
docker compose logs --no-color openmrs
docker compose logs --no-color database
```

For a failed or interrupted first initialization, remove the partial database
before retrying:

```bash
docker compose down --volumes --remove-orphans
docker compose up --detach --wait --wait-timeout 1200
```

Do not preserve a database from an interrupted Liquibase run.

### Port 8080 is already in use

Stop the conflicting process or override the published port in a local Compose
override file. If the port changes, override both OpenMRS base URLs when running
the tests.

### Browser does not start

Verify that Chrome or Firefox is installed and that `BROWSER` contains
`chrome` or `firefox`. Selenium Manager requires network access the first time
it resolves a driver.

### Login fails

Confirm that the Compose service is healthy and that the configured username,
password and login location match the target environment. An authenticated
request can be checked directly:

```bash
curl --fail --user admin:Admin123 \
  http://localhost:8080/openmrs/ws/rest/v1/session
```

### Public demo returns HTTP 403 or `Just a moment...`

This is an upstream Cloudflare decision, not an OpenMRS functional failure. Use
the controlled Compose target for deterministic execution.
