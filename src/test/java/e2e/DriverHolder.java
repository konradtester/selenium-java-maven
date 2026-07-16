package e2e;

import org.openqa.selenium.WebDriver;

/**
 * Lazily creates the WebDriver on first use, so API-only scenarios never launch a browser
 * (mirrors how the "driver" pytest fixture is only resolved when a UI step actually needs it).
 */
public class DriverHolder {

    private WebDriver driver;

    public WebDriver getDriver() {
        if (driver == null) {
            driver = DriverFactory.create();
        }
        return driver;
    }

    public void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
