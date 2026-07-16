package e2e.pages;

import e2e.DriverHolder;
import e2e.TestContext;

/**
 * Page Object containing selectors and actions for the login page.
 */
public class LoginPage extends BasePage {

    public LoginPage(DriverHolder driverHolder, TestContext context) {
        super(driverHolder, context);
    }

    public void login(String username, String password) {
        fillInput("Username Field", username);
        fillInput("Password Field", password);
        clickButton("Login Button");
    }

    public void checkErrorMessage(String message) {
        assertVisible(message);
    }
}
