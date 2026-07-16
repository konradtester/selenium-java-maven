package e2e.steps;

import e2e.ApiContext;
import e2e.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ApiSteps {

    private final ApiContext api;
    private final TestContext context;

    public ApiSteps(ApiContext api, TestContext context) {
        this.api = api;
        this.context = context;
    }

    // ==========================================================================
    // REQUEST SETUP (headers & body)
    // ==========================================================================

    @Given("I set the request header {string} to {string}")
    public void setRequestHeader(String name, String value) {
        api.getHeaders().put(name, api.resolve(value));
    }

    @Given("I set the request body to:")
    public void setRequestBody(String docString) {
        api.setBody(api.resolve(docString));
    }

    @Given("I set the request body from file {string}")
    public void setRequestBodyFromFile(String filename) {
        api.setBody(loadJsonFixture(filename));
    }

    // ==========================================================================
    // SENDING REQUESTS
    // ==========================================================================

    @When("I send a {string} request to {string}")
    public void sendRequest(String method, String path) {
        api.send(method, path);
    }

    @When("I send a {string} request to {string} with body:")
    public void sendRequestWithBody(String method, String path, String docString) {
        api.setBody(api.resolve(docString));
        api.send(method, path);
    }

    @When("I send a {string} request to {string} with body from file {string}")
    public void sendRequestWithBodyFromFile(String method, String path, String filename) {
        api.setBody(loadJsonFixture(filename));
        api.send(method, path);
    }

    // ==========================================================================
    // RESPONSE ASSERTIONS
    // ==========================================================================

    @Then("the response status code should be {int}")
    public void statusCodeShouldBe(int status) {
        if (api.getResponse() == null) {
            throw new IllegalStateException("No response received yet. Send a request first.");
        }
        int actual = api.getResponse().statusCode();
        if (actual != status) {
            throw new AssertionError(String.format(
                    "Expected status %d, got %d. Body: %s", status, actual, api.getResponse().body()
            ));
        }
    }

    @Then("the response body should contain {string}")
    public void responseBodyShouldContain(String text) {
        String resolved = api.resolve(text);
        if (!api.getResponse().body().contains(resolved)) {
            throw new AssertionError(
                    "Expected response body to contain '" + resolved + "'. Body: " + api.getResponse().body()
            );
        }
    }

    @Then("the response field {string} should equal {string}")
    public void responseFieldShouldEqual(String fieldPath, String expected) {
        Object actual = api.getField(fieldPath);
        String resolvedExpected = api.resolve(expected);
        if (!String.valueOf(actual).equals(resolvedExpected)) {
            throw new AssertionError(String.format(
                    "Expected field '%s' to equal '%s', got '%s'.", fieldPath, resolvedExpected, actual
            ));
        }
    }

    @Then("the response field {string} should contain {string}")
    public void responseFieldShouldContain(String fieldPath, String expected) {
        Object actual = api.getField(fieldPath);
        String resolvedExpected = api.resolve(expected);
        if (!String.valueOf(actual).contains(resolvedExpected)) {
            throw new AssertionError(String.format(
                    "Expected field '%s' to contain '%s', got '%s'.", fieldPath, resolvedExpected, actual
            ));
        }
    }

    // ==========================================================================
    // SAVING RESPONSE DATA AS VARIABLES
    // ==========================================================================

    @When("I save the response field {string} as variable {string}")
    public void saveResponseField(String fieldPath, String varName) {
        context.getState().put(varName, String.valueOf(api.getField(fieldPath)));
    }

    /**
     * Reads a JSON fixture from src/test/resources/data/ (on the test classpath),
     * resolving {{variable}} placeholders in its raw text before returning it.
     */
    private String loadJsonFixture(String filename) {
        String resourcePath = "/data/" + filename;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Request body fixture not found on classpath: " + resourcePath);
            }
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return api.resolve(raw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fixture: " + resourcePath, e);
        }
    }
}
