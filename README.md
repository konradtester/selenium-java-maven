# ☕ E2E Starter (Java + Maven + Selenium + Cucumber-JVM)

A minimal, general-purpose starter for end-to-end (E2E) testing, built for fast onboarding of new testers — no prior programming experience required to write scenarios.

The project is built on a standard Java testing stack: **Maven**, **Selenium WebDriver**, and **Cucumber-JVM** (Gherkin support) run through the **JUnit 5 Platform**.

Out of the box it points at a public demo site (https://the-internet.herokuapp.com) and a public demo API (https://jsonplaceholder.typicode.com) so you can clone it and run passing tests immediately, then swap in your own application's URL, credentials, and selectors.

This is a Java/Maven port of the [`selenium_python`](../selenium_python/) starter — the Gherkin vocabulary (step wording) is identical, so scenarios are portable between the two.

---

## 🧱 Project Structure

```
selenium_java_maven/
├── .env                        # Environment variables (URLs, credentials) — not committed
├── .env.example                 # Template for .env
├── pom.xml                      # Maven build + dependencies
└── src/test/
    ├── java/e2e/
    │   ├── RunCucumberTest.java  # JUnit 5 entry point that runs every scenario
    │   ├── Hooks.java            # Scenario lifecycle (closes the browser after each scenario)
    │   ├── EnvConfig.java        # Loads .env once (mirrors python-dotenv)
    │   ├── TestContext.java      # Resolves {{variable}} placeholders, shared per scenario
    │   ├── DriverFactory.java    # Builds the Chrome/Firefox WebDriver
    │   ├── DriverHolder.java     # Lazily creates the driver (API-only scenarios skip it)
    │   ├── ApiContext.java       # Session state (headers, body, response) for API steps
    │   ├── pages/                # Page Object Model
    │   │   ├── BasePage.java
    │   │   ├── LoginPage.java
    │   │   └── locators/         # Central registry of friendly locator names, split by area
    │   │       ├── AuthLocators.java
    │   │       ├── AppLocators.java
    │   │       ├── IntegrationsLocators.java
    │   │       └── LocatorRegistry.java
    │   └── steps/                 # Step definitions matched to scenarios
    │       ├── GenericSteps.java  # UI (Selenium) steps
    │       ├── ApiSteps.java      # API/REST steps
    │       └── EmailSteps.java    # Email/IMAP steps
    └── resources/
        ├── features/              # Test scenarios in plain language (Gherkin/Cucumber)
        │   ├── login.feature
        │   └── api_example.feature
        └── data/                   # JSON request-body fixtures for API steps
            └── create_post.json
```

---

## ⚙️ 1. Szybki Start (macOS & Windows)

### Wymagania wstępne:
* JDK 17 lub nowsza (`java -version`)
* Maven 3.9+ (lub wbudowany Maven z Twojego IDE)
* Zainstalowana lokalnie przeglądarka Chrome lub Firefox (Selenium Manager automatycznie pobierze dopasowany sterownik)

### Konfiguracja środowiska (`.env`)

Skopiuj szablon `.env.example` jako `.env`:
* **macOS/Linux**:
  ```bash
  cp .env.example .env
  ```
* **Windows**:
  ```cmd
  copy .env.example .env
  ```

Domyślne ustawienia są od razu skonfigurowane do pracy z publiczną stroną demonstracyjną.

### Kompilacja:
```bash
mvn compile
```

---

## 🏃 2. Uruchamianie Testów

```bash
mvn test
```

### Uruchomienie w tle (Headless):
```bash
mvn test -Dheadless=true
```

### Wybór innej przeglądarki (np. Firefox):
Domyślną przeglądarką jest `chrome`; `firefox` jest również obsługiwany:
```bash
mvn test -Dbrowser=firefox
```

### Uruchomienie konkretnego scenariusza lub tagu:
Możesz użyć filtrowania po tagach Cucumber za pomocą właściwości systemowej:
```bash
mvn test -Dcucumber.filter.tags="@smoke"
```
*(Wystarczy dodać `@smoke` nad scenariuszem w pliku `.feature`).*

*Wskazówka: Większość środowisk IDE (np. IntelliJ, VS Code) pozwala na uruchamianie pojedynczych scenariuszy bezpośrednio z edytora za pomocą ikony "Run" obok kodu.*

---

## 💎 3. Dynamic Variables in Scenarios

Identical mechanism to the Python starter — `{{variable}}` placeholders in Gherkin steps are resolved from `.env` or dynamically generated data:

| Placeholder | Description | Resolved value |
| :--- | :--- | :--- |
| `{{username}}` | Login username | `USERNAME` from `.env` |
| `{{password}}` | Login password | `PASSWORD` from `.env` |
| `{{base_url}}` | Application base URL | `BASE_URL` from `.env` |
| `{{test_email}}` | **Dynamic test email** | Generates a random email (e.g. `test+a8j1kd89@example.com`). **Stable for the whole scenario.** |

Any variable saved during a scenario (see below) also becomes available as `{{var_name}}` for the rest of that scenario.

---

## 🎯 4. Where do locators go?

To avoid hardcoding technical CSS/XPath selectors directly in `.feature` files, the locator registry is split into logical classes under `src/test/java/e2e/pages/locators/`:

* **`AuthLocators.java`**: login, registration, account locators.
* **`AppLocators.java`**: the authenticated part of the app (dashboard, navigation, settings).
* **`IntegrationsLocators.java`**: third-party integrations (payment gateways, external widgets, sandboxes).

All of these maps are merged automatically in `LocatorRegistry.java`. In a `.feature` file, use the friendly name (e.g. `"Username Field"`) instead of the raw selector — `BasePage` checks the registry first, then falls back to searching by text, placeholder, or label.

---

## 🤝 5. Writing New Tests (UI)

In most cases you don't need to write any Java code to add a new UI scenario:

1. Create a new `.feature` file under `src/test/resources/features/`.
2. Use the ready-made steps from `GenericSteps.java`, e.g.:
   * `Given I navigate to "/path"`
   * `When I fill the field "Friendly field name or selector" with "Value"`
   * `When I click the button "Friendly button name or selector"`
   * `Then I should see the text or element "Friendly name or selector"`
   * `Then the page URL should end with "/path"`
   * `Then I wait 2000 ms`
3. Run `mvn test` — the scenario is discovered and executed automatically.

---

## 🔌 6. API / REST Tests

Alongside the Selenium (UI) steps, the starter includes generic steps for testing a JSON REST API directly, defined in `ApiSteps.java`. These scenarios never launch a browser (see `DriverHolder`'s lazy creation), so they run fast and can live in the same `features/` folder as UI tests.

Requests resolve relative paths against `API_BASE_URL` from `.env` (falls back to `BASE_URL`), and support the same `{{variable}}` placeholders as UI steps.

Request bodies live as JSON fixtures under `src/test/resources/data/` rather than inline in the scenario:

```json
// src/test/resources/data/create_post.json
{
  "title": "foo",
  "body": "bar",
  "userId": 1,
  "authorEmail": "{{test_email}}"
}
```

```gherkin
Feature: API / REST requests

  Scenario: Create a resource from a JSON fixture and reuse its id
    Given I set the request header "Content-Type" to "application/json"
    When I send a "POST" request to "/posts" with body from file "create_post.json"
    Then the response status code should be 201
    And the response field "title" should equal "foo"
    When I save the response field "id" as variable "new_post_id"
    And I send a "GET" request to "/posts/{{new_post_id}}"
```

Available steps:
* `Given I set the request header "NAME" to "VALUE"`
* `Given I set the request body from file "FILENAME"` (loads a fixture from `src/test/resources/data/`, resolving `{{variables}}`)
* `Given I set the request body to:` (inline JSON docstring, for quick one-offs)
* `When I send a "METHOD" request to "PATH"` (e.g. `"GET"`, `"POST"`, `"PUT"`, `"DELETE"`)
* `When I send a "METHOD" request to "PATH" with body from file "FILENAME"`
* `When I send a "METHOD" request to "PATH" with body:` (inline JSON docstring)
* `Then the response status code should be STATUS`
* `Then the response body should contain "TEXT"`
* `Then the response field "FIELD.PATH" should equal "VALUE"` (dot path, e.g. `data.0.id`)
* `Then the response field "FIELD.PATH" should contain "VALUE"`
* `When I save the response field "FIELD.PATH" as variable "VAR_NAME"`

See `src/test/resources/features/api_example.feature` for a full working example.

---

## 📧 7. Email Verification Steps

`EmailSteps.java` provides IMAP-based steps for verifying emails sent by your app (registration confirmations, password resets, etc.). Requires `IMAP_HOST`, `IMAP_USER`, and `IMAP_PASSWORD` in `.env`.

* `Then I wait for email sent to "ADDRESS" containing "TEXT"`
* `Then I save the link matching "PATTERN" from email sent to "ADDRESS" as variable "VAR_NAME"`

---

## 🏆 8. Good Practices

* **Write business-readable scenarios**: reflect real user journeys, and use friendly locator names from the registry rather than raw selectors.
* **Single source of truth for config**: keep credentials and URLs out of `.feature` files. Use placeholders like `{{username}}` and `{{password}}`, resolved automatically from `.env` at runtime.
* **Keep request bodies as fixtures**: put JSON payloads in `src/test/resources/data/` instead of inlining them in scenarios, so both are easy to review and reuse.
