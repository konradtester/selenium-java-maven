package e2e;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Configures and creates a Selenium WebDriver instance. Defaults to Chrome, also supports
 * Firefox, and respects the "headless"/"browser" system properties (see pom.xml surefire config,
 * overridable with -Dheadless=true -Dbrowser=firefox).
 *
 * Selenium 4 automatically fetches the matching driver binary via Selenium Manager, so no
 * separate driver-management dependency is needed.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));
        String browser = System.getProperty("browser", "chrome").toLowerCase();

        switch (browser) {
            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                options.addArguments("--width=1280", "--height=1000");
                return new FirefoxDriver(options);
            }
            case "chrome": {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,1000");
                return new ChromeDriver(options);
            }
            default:
                throw new IllegalArgumentException("Browser '" + browser + "' is not supported.");
        }
    }
}
