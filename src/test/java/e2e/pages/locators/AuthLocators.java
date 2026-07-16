package e2e.pages.locators;

import java.util.Map;

/**
 * Locators related to authentication flows: login, registration, password reset.
 * The defaults below point at the public demo site https://the-internet.herokuapp.com/login
 * so the sample scenario works out of the box. Replace them with your own app's selectors.
 */
public final class AuthLocators {

    private AuthLocators() {
    }

    public static final Map<String, String> LOCATORS = Map.of(
            "Username Field", "#username",
            "Password Field", "#password",
            "Login Button", "button[type='submit']",
            "Flash Message", "#flash"
    );
}
