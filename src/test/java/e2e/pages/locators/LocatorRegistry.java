package e2e.pages.locators;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges all partial locator maps into a single central registry that BasePage uses to
 * resolve friendly locator names.
 */
public final class LocatorRegistry {

    private LocatorRegistry() {
    }

    public static final Map<String, String> LOCATORS = new LinkedHashMap<>();

    static {
        LOCATORS.putAll(AuthLocators.LOCATORS);
        LOCATORS.putAll(AppLocators.LOCATORS);
        LOCATORS.putAll(IntegrationsLocators.LOCATORS);
    }
}
