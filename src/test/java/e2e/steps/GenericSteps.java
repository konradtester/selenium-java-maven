package e2e.steps;

import e2e.DriverHolder;
import e2e.EnvConfig;
import e2e.TestContext;
import e2e.pages.BasePage;
import e2e.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class GenericSteps {

    private final BasePage helper;
    private final LoginPage loginPage;
    private final TestContext context;

    public GenericSteps(DriverHolder driverHolder, TestContext context) {
        this.helper = new BasePage(driverHolder, context);
        this.loginPage = new LoginPage(driverHolder, context);
        this.context = context;
    }

    // ==========================================================================
    // NAVIGATION
    // ==========================================================================

    @Given("I navigate to {string}")
    public void iNavigateTo(String url) {
        helper.goTo(url);
    }

    @Given("I am on the main page")
    public void iAmOnTheMainPage() {
        helper.goTo("/");
    }

    @Then("the page URL should end with {string}")
    public void urlShouldEndWith(String endpoint) {
        helper.verifyUrlEndsWith(endpoint);
    }

    @Then("I should be on the {string} page")
    public void shouldBeOnPage(String endpoint) {
        helper.verifyUrlEndsWith(endpoint);
    }

    @Then("the page URL should contain {string}")
    public void urlShouldContain(String text) {
        helper.verifyUrlContains(text);
    }

    @Then("the page URL should equal {string}")
    public void urlShouldEqual(String url) {
        helper.verifyUrlEquals(url);
    }

    // ==========================================================================
    // CLICKS & INTERACTIONS
    // ==========================================================================

    @When("I click the link containing {string}")
    public void clickLinkContaining(String href) {
        helper.clickHref(href);
    }

    @When("I click the button {string}")
    public void clickButton(String buttonTextOrSelector) {
        helper.clickButton(buttonTextOrSelector);
    }

    @When("I click the element with text {string}")
    public void clickElementWithText(String text) {
        helper.clickText(text);
    }

    @When("I accept cookies with button {string}")
    public void acceptCookies(String buttonTextOrSelector) {
        helper.tryClickCookieBanner(buttonTextOrSelector);
    }

    // ==========================================================================
    // FORM FILLING & SELECTIONS
    // ==========================================================================

    @When("I fill the field {string} with {string}")
    public void fillField(String field, String value) {
        helper.fillInput(field, value);
    }

    @When("I check the checkbox {string}")
    public void checkCheckbox(String selector) {
        helper.checkCheckbox(selector, true);
    }

    @When("I uncheck the checkbox {string}")
    public void uncheckCheckbox(String selector) {
        helper.checkCheckbox(selector, false);
    }

    @When("I select option {string} from {string}")
    public void selectOption(String option, String selector) {
        helper.selectOption(selector, option);
    }

    // ==========================================================================
    // ASSERTIONS & VERIFICATION
    // ==========================================================================

    @Then("I should see the text or element {string}")
    public void shouldSeeElement(String selectorOrText) {
        helper.assertVisible(selectorOrText);
    }

    @Then("I should not see the text or element {string}")
    public void shouldNotSeeElement(String selectorOrText) {
        helper.assertHidden(selectorOrText);
    }

    @Then("the text of {string} should contain {string}")
    public void textShouldContain(String selector, String expectedText) {
        String actual = helper.getText(selector);
        helper.assertContains(actual, expectedText);
    }

    @Then("the value of {string} should equal {string}")
    public void valueShouldEqual(String selector, String expectedText) {
        String actual = helper.getText(selector);
        helper.assertEquals(actual.strip(), expectedText.strip());
    }

    @Then("assert data {string} equals {string}")
    public void assertDataEquals(String data1, String data2) {
        helper.assertEquals(data1, data2);
    }

    @Then("assert data {string} contains {string}")
    public void assertDataContains(String data1, String data2) {
        helper.assertContains(data1, data2);
    }

    // ==========================================================================
    // TIMERS & WAITING
    // ==========================================================================

    @Then("I wait {int} ms")
    public void waitMilliseconds(int ms) {
        helper.waitForTimeout(ms);
    }

    // ==========================================================================
    // LOGIN STEPS
    // ==========================================================================

    @When("I login with username {string} and password {string}")
    public void loginWithCredentials(String username, String password) {
        loginPage.login(username, password);
    }

    @When("I login as {string} user")
    public void loginAsRole(String role) {
        String envUsernameKey = role.toUpperCase() + "_USERNAME";
        String envPasswordKey = role.toUpperCase() + "_PASSWORD";

        String username = EnvConfig.get(envUsernameKey);
        String password = EnvConfig.get(envPasswordKey);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalStateException(
                    "Missing credentials for role '" + role + "' in the .env file ("
                            + envUsernameKey + " / " + envPasswordKey + ")."
            );
        }

        loginPage.login(username, password);
    }

    @Then("I should see an error message {string}")
    public void shouldSeeErrorMessage(String message) {
        loginPage.checkErrorMessage(message);
    }

    // ==========================================================================
    // SAVING CONTEXT VARIABLES
    // ==========================================================================

    @When("I save the text of {string} as variable {string}")
    public void saveTextAsVariable(String selectorOrFriendlyName, String varName) {
        String text = helper.getText(selectorOrFriendlyName);
        context.getState().put(varName, text.strip());
    }
}
