package e2e.pages;

import e2e.DriverHolder;
import e2e.EnvConfig;
import e2e.TestContext;
import e2e.pages.locators.LocatorRegistry;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Base class for the Page Object Model, containing common actions and assertions.
 */
public class BasePage {

    private final DriverHolder driverHolder;
    private final TestContext context;
    protected final Duration timeout = Duration.ofSeconds(10);

    public BasePage(DriverHolder driverHolder, TestContext context) {
        this.driverHolder = driverHolder;
        this.context = context;
    }

    protected WebDriver driver() {
        return driverHolder.getDriver();
    }

    /** Resolves dynamic context variables, e.g. {{username}}, {{test_email}}, etc. */
    public String resolve(String text) {
        return context.resolve(text);
    }

    /** Checks the central locator registry for a friendly name; falls back to the raw value. */
    public String resolveLocator(String locatorName) {
        String resolvedName = resolve(locatorName);
        return LocatorRegistry.LOCATORS.getOrDefault(resolvedName, resolvedName);
    }

    private boolean isXPath(String selector) {
        return selector.startsWith("/") || selector.startsWith("./") || selector.startsWith("(/");
    }

    private boolean isCss(String selector) {
        return selector.startsWith("#") || selector.startsWith(".") || selector.contains("[") || selector.contains(":");
    }

    public void goTo(String url) {
        String resolvedUrl = resolve(url);
        if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
            resolvedUrl = withBaseUrl(resolvedUrl);
        }
        driver().get(resolvedUrl);
    }

    private String withBaseUrl(String path) {
        String baseUrl = EnvConfig.get("BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("BASE_URL is not set in the environment variables.");
        }
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmedPath = path.startsWith("/") ? path.substring(1) : path;
        return trimmedBase + "/" + trimmedPath;
    }

    /** Locates an element via CSS selector, XPath or text, resolving names through the registry first. */
    public WebElement findElement(String selectorOrText) {
        return findElement(selectorOrText, timeout);
    }

    public WebElement findElement(String selectorOrText, Duration t) {
        String resolved = resolveLocator(selectorOrText);
        WebDriverWait wait = new WebDriverWait(driver(), t);

        if (isXPath(resolved)) {
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(resolved)));
        } else if (isCss(resolved)) {
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resolved)));
        } else {
            String xpath = String.format(
                    "//*[contains(normalize-space(text()), '%1$s') or (contains(normalize-space(.), '%1$s') and not(*))]",
                    resolved
            );
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        }
    }

    /** Scrolls into view and clicks; falls back to a JS click if intercepted by another element. */
    public void clickElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            element.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
        }
    }

    public void click(String selector) {
        clickElement(findElement(selector));
    }

    public void fill(String selector, String text) {
        String resolvedText = resolve(text);
        WebElement element = findElement(selector);
        element.clear();
        element.sendKeys(resolvedText);
    }

    public void verifyUrlEndsWith(String endpoint) {
        String resolvedEndpoint = resolve(endpoint);
        new WebDriverWait(driver(), timeout).until(d -> d.getCurrentUrl().endsWith(resolvedEndpoint));
    }

    public void verifyUrlContains(String text) {
        String resolvedText = resolve(text);
        new WebDriverWait(driver(), timeout).until(d -> d.getCurrentUrl().contains(resolvedText));
    }

    public void verifyUrlEquals(String expectedUrl) {
        String resolvedUrl = resolve(expectedUrl);
        if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
            resolvedUrl = withBaseUrl(resolvedUrl);
        }
        String finalUrl = resolvedUrl;
        new WebDriverWait(driver(), timeout).until(d -> d.getCurrentUrl().equals(finalUrl));
    }

    /** Clicks a link (&lt;a&gt; tag) containing the given href fragment. */
    public void clickHref(String href) {
        String resolvedHref = resolve(href);
        String xpath = String.format("//a[contains(@href, '%s')]", resolvedHref);
        WebElement element = new WebDriverWait(driver(), timeout)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        clickElement(element);
    }

    /**
     * Clicks a button based on text or a selector (resolves the registry first). Prioritizes
     * &lt;button&gt; tags, input fields, &lt;a&gt; links, then any other element.
     */
    public void clickButton(String textOrSelector) {
        String resolved = resolveLocator(textOrSelector);

        if (isXPath(resolved)) {
            WebElement element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(resolved)));
            clickElement(element);
            return;
        }
        if (isCss(resolved)) {
            WebElement element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector(resolved)));
            clickElement(element);
            return;
        }

        String[] xpathPriorities = {
                String.format("//button[normalize-space()='%s']", resolved),
                String.format("//button[contains(text(), '%s')]", resolved),
                String.format("//input[@type='submit' and contains(@value, '%s')]", resolved),
                String.format("//input[@type='button' and contains(@value, '%s')]", resolved),
                String.format("//a[normalize-space()='%s']", resolved),
                String.format("//a[contains(text(), '%s')]", resolved),
                String.format("//*[contains(text(), '%s')]", resolved)
        };

        WebElement element = null;
        for (String xpath : xpathPriorities) {
            List<WebElement> elements = driver().findElements(By.xpath(xpath));
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    element = el;
                    break;
                }
            }
            if (element != null) {
                break;
            }
        }

        if (element == null) {
            String fallbackXpath = String.format(
                    "//button[contains(text(), '%1$s')] | //*[contains(text(), '%1$s')]", resolved
            );
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(fallbackXpath)));
        }

        clickElement(element);
    }

    public void clickText(String text) {
        String resolved = resolveLocator(text);
        String xpath = String.format("//*[contains(text(), '%s')]", resolved);
        WebElement element = new WebDriverWait(driver(), timeout)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        clickElement(element);
    }

    /**
     * Best-effort click for cookie banners: waits at most 2s and silently succeeds if the
     * banner never appears.
     */
    public void tryClickCookieBanner(String textOrSelector) {
        String resolved = resolve(textOrSelector);
        try {
            WebElement element;
            Duration shortTimeout = Duration.ofSeconds(2);
            if (isCss(resolved)) {
                element = new WebDriverWait(driver(), shortTimeout)
                        .until(ExpectedConditions.elementToBeClickable(By.cssSelector(resolved)));
            } else {
                String xpath = String.format(
                        "//button[contains(text(), '%1$s')] | //*[contains(text(), '%1$s') or (contains(., '%1$s') and not(*))]",
                        resolved
                );
                element = new WebDriverWait(driver(), shortTimeout)
                        .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            }
            clickElement(element);
            waitForTimeout(500);
        } catch (Exception ignored) {
            // Ignore if the banner isn't present on the page
        }
    }

    /**
     * Types text into an input field. Resolves the field through the locator registry, then
     * searches by CSS selector, placeholder, id, name, or associated label.
     */
    public void fillInput(String selectorOrPlaceholder, String text) {
        String resolvedSelector = resolveLocator(selectorOrPlaceholder);
        String resolvedText = resolve(text);
        WebElement element;

        if (isXPath(resolvedSelector)) {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(resolvedSelector)));
        } else if (isCss(resolvedSelector)) {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resolvedSelector)));
        } else {
            String xpath = String.format(
                    "//input[contains(@placeholder, '%1$s')] | "
                            + "//input[contains(@id, '%1$s')] | "
                            + "//input[contains(@name, '%1$s')] | "
                            + "//textarea[contains(@placeholder, '%1$s')] | "
                            + "//label[contains(text(), '%1$s')]/following-sibling::input | "
                            + "//label[contains(text(), '%1$s')]/input",
                    resolvedSelector
            );
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        }

        element.clear();
        element.sendKeys(resolvedText);
    }

    /** Selects an option from a dropdown by visible text, falling back to value. */
    public void selectOption(String selector, String value) {
        String resolvedSelector = resolveLocator(selector);
        String resolvedValue = resolve(value);
        WebElement element;

        if (isXPath(resolvedSelector)) {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(resolvedSelector)));
        } else {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resolvedSelector)));
        }

        Select select = new Select(element);
        try {
            select.selectByVisibleText(resolvedValue);
        } catch (Exception e) {
            select.selectByValue(resolvedValue);
        }
    }

    public void checkCheckbox(String selectorOrLabel, boolean check) {
        String resolved = resolveLocator(selectorOrLabel);
        WebElement element;

        if (isXPath(resolved)) {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(resolved)));
        } else if (isCss(resolved)) {
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resolved)));
        } else {
            String xpath = String.format(
                    "//label[contains(text(), '%1$s')]/preceding-sibling::input[@type='checkbox'] | "
                            + "//label[contains(text(), '%1$s')]/following-sibling::input[@type='checkbox'] | "
                            + "//label[contains(text(), '%1$s')]/input[@type='checkbox'] | "
                            + "//input[@type='checkbox' and @id=//label[contains(text(), '%1$s')]/@for]",
                    resolved
            );
            element = new WebDriverWait(driver(), timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        }

        boolean isSelected = element.isSelected();
        if ((check && !isSelected) || (!check && isSelected)) {
            clickElement(element);
        }
    }

    public void waitForTimeout(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getText(String selector) {
        return findElement(selector).getText();
    }

    // --- ASSERTIONS ---

    public void assertEquals(String actual, String expected) {
        String resolvedExpected = resolve(expected);
        if (!Objects.equals(actual, resolvedExpected)) {
            throw new AssertionError(String.format("Expected '%s', but got '%s'", resolvedExpected, actual));
        }
    }

    public void assertContains(String actual, String expectedSub) {
        String resolvedSub = resolve(expectedSub);
        if (actual == null || !actual.contains(resolvedSub)) {
            throw new AssertionError(String.format("Expected substring '%s' was not found in '%s'", resolvedSub, actual));
        }
    }

    public void assertVisible(String selectorOrText) {
        String resolved = resolveLocator(selectorOrText);
        try {
            if (isXPath(resolved)) {
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(resolved)));
            } else if (isCss(resolved)) {
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(resolved)));
            } else {
                String xpath = String.format(
                        "//*[contains(normalize-space(text()), '%1$s') or (contains(normalize-space(.), '%1$s') and not(*))]",
                        resolved
                );
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
            }
        } catch (Exception e) {
            throw new AssertionError(String.format(
                    "Element or text '%s' was not visible within %ds: %s", resolved, timeout.getSeconds(), e.getMessage()
            ));
        }
    }

    public void assertHidden(String selectorOrText) {
        String resolved = resolveLocator(selectorOrText);
        try {
            if (isXPath(resolved)) {
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(resolved)));
            } else if (isCss(resolved)) {
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(resolved)));
            } else {
                String xpath = String.format(
                        "//*[contains(normalize-space(text()), '%1$s') or (contains(normalize-space(.), '%1$s') and not(*))]",
                        resolved
                );
                new WebDriverWait(driver(), timeout).until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
            }
        } catch (Exception e) {
            throw new AssertionError(String.format(
                    "Element or text '%s' did not disappear within %ds: %s", resolved, timeout.getSeconds(), e.getMessage()
            ));
        }
    }
}
