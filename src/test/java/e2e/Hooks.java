package e2e;

import io.cucumber.java.After;

/**
 * Scenario lifecycle hooks. Only closes the browser if one was actually launched
 * (API-only scenarios never create a WebDriver, so this is a no-op for them).
 */
public class Hooks {

    private final DriverHolder driverHolder;

    public Hooks(DriverHolder driverHolder) {
        this.driverHolder = driverHolder;
    }

    @After
    public void tearDown() {
        driverHolder.quit();
    }
}
