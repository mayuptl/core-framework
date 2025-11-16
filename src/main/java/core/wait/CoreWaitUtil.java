package core.wait;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
/**
 * Utility class to encapsulate explicit wait functionality using Selenium's {@link WebDriverWait}.
 * This class provides methods for common wait conditions like visibility, clickability, and page load state.
 */
public class CoreWaitUtil {

    private final WebDriver driver;
    /**
     * Constructs a WaitUtil instance.
     * @param driver The WebDriver instance to use for all wait operations.
     */
    public CoreWaitUtil(WebDriver driver)
    {
        this.driver = driver;
    }
    /**
     * Creates a WebDriverWait instance with the specified timeout duration.
     *
     * @param timeOutInSec The maximum time in seconds to wait for a condition.
     * @return A configured WebDriverWait instance.
     */
    private WebDriverWait getWait(int timeOutInSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeOutInSec));
    }

    /** Hard stop, same as Thread sleep */
    public static void staticWait(int seconds)
    {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(seconds));
    }
    /**
     * Waits for a specific WebElement to be visible on the page.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param element The WebElement to wait for visibility.
     * @return The visible WebElement.
     */
    public WebElement waitForVisibilityOf(int timeOutInSec, WebElement element) {
       return getWait(timeOutInSec).until(ExpectedConditions.visibilityOf(element));
    }
    /**
     * Waits for an element to be visible using its {@link By} locator.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param locator The {@link By} locator of the element.
     * @return The visible WebElement.
     */
    public WebElement waitForVisibilityOfLocated(int timeOutInSec, By locator) {
        return getWait(timeOutInSec).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    /**
     * Waits for all elements in a list to be visible on the page.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param elements The List of WebElements to wait for.
     */
    public void waitForVisibilityOfAll(int timeOutInSec, List<WebElement> elements) {
        getWait(timeOutInSec).until(ExpectedConditions.visibilityOfAllElements(elements));
    }
    /** Wait for at least one element in list to be visible */
    public void waitForVisibilityOfAtLeastOne(int timeOutInSec, List<WebElement> elements) {
        getWait(timeOutInSec).until(d -> {
            try {
                return elements.stream().anyMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
        //getWait(timeOutInSec).until(ExpectedConditions.visibilityOfAnyElements(elements));
    }
    /**
     * Waits for a specific WebElement to become invisible (not displayed) on the page.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param element The WebElement to wait for invisibility.
     */
    public void waitForInVisibilityOf(int timeOutInSec, WebElement element) {
        getWait(timeOutInSec).until(ExpectedConditions.invisibilityOf(element));
    }
    /**
     * Waits for all elements in a list to become invisible (or removed from the DOM).
     * This iterates through the list, waiting for each element to disappear.
     *
     * @param timeOutInSec The maximum time in seconds to wait for each element.
     * @param elements The List of WebElements to wait for invisibility.
     */
    public void waitForInVisibilityOfAll(int timeOutInSec, List<WebElement> elements) {
        for (WebElement element : elements) {
            try {
                getWait(timeOutInSec).until(ExpectedConditions.invisibilityOf(element));
            } catch (StaleElementReferenceException ignored) {
                // If element is stale, it's already gone — no need to wait further
            }
        }
        //getWait(timeOutInSec).until(ExpectedConditions.invisibilityOfAllElements(elements));
    }
    /**
     * Waits for a specific WebElement to be clickable (visible and enabled).
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param element The WebElement to wait for clickability.
     */
    public void waitForToBeClickable(int timeOutInSec, WebElement element) {
        getWait(timeOutInSec).until(ExpectedConditions.elementToBeClickable(element));
    }
    /**
     * Waits for all elements in a list to be clickable.
     *
     * @param timeOutInSec The maximum time in seconds to wait for each element.
     * @param elements The List of WebElements to wait for clickability.
     */
    public void waitForToBeClickableAll(int timeOutInSec, List<WebElement> elements) {
        for (WebElement element : elements) {
            getWait(timeOutInSec).until(ExpectedConditions.elementToBeClickable(element));
        }
    }
    /**
     * Waits for the page URL to be exactly the specified URL.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param url The exact URL expected.
     */
    public void waitForUrlToBe(int timeOutInSec, String url)
    {
        getWait(timeOutInSec).until(ExpectedConditions.urlContains(url));
    }
    /**
     * Waits for the text to be present in the specified WebElement.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param element The WebElement to check for text.
     * @param text The exact text expected to be present.
     */
    public void waitForTextToBePresentIn(int timeOutInSec, WebElement element, String text) {
        getWait(timeOutInSec).until(ExpectedConditions.textToBePresentInElement(element, text));
    }
    /**
     * Waits for an alert pop-up to be present.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     */
    public void waitForAlert(int timeOutInSec) {
        getWait(timeOutInSec).until(ExpectedConditions.alertIsPresent());
    }
    /**
     * Waits for the page title to contain a specific text fragment.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param title The text fragment expected in the page title.
     */
    public void waitForTitleContains(int timeOutInSec, String title) {
        getWait(timeOutInSec).until(ExpectedConditions.titleContains(title));
    }
    /**
     * Waits for the current URL to contain a specific fragment.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param urlFragment The URL fragment expected to be present.
     */
    public void waitForUrlContains(int timeOutInSec, String urlFragment) {
        getWait(timeOutInSec).until(ExpectedConditions.urlContains(urlFragment));
    }
    /**
     * Waits for a specific attribute of an element to have a defined value.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @param element The WebElement to check the attribute on.
     * @param attribute The name of the attribute (e.g., "class", "value").
     * @param value The expected value of the attribute.
     */
    public void waitForAttributeToBe(int timeOutInSec, WebElement element, String attribute, String value) {
        getWait(timeOutInSec).until(ExpectedConditions.attributeToBe(element, attribute, value));
    }
    /**
     * Waits for the page to load completely using JavaScript's `document.readyState`.
     *
     * @param timeOutInSec The maximum time in seconds to wait.
     * @return {@code true} if the page loads successfully, {@code false} otherwise.
     */
    public boolean waitForPageLoad(int timeOutInSec)
    {
        try {
            // Add a custom message to the TimeoutException for better debugging
            getWait(timeOutInSec).withMessage("Timeout waiting for page to load completely (readyState='complete').")
                    .until((WebDriver d) -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));
            // log.info("Page loaded successfully after waiting for " + timeOutInSec + " seconds.");
            return true; // Return true on success
        } catch (TimeoutException e) {
            // Log a specific message for the expected timeout
            System.out.println("Page did not load within " + timeOutInSec + " seconds. TimeoutException: " + e.getMessage());
            return false; // Return false on failure
        } catch (Exception e) {
            // Catch any other unexpected exceptions (e.g., driver issues)
            System.out.println("An unexpected error occurred while waiting for page load: " + e.getMessage());
            return false; // Return false for unexpected errors
        }
    }
    /** Static method to Wait for the page load, Need driver as arg and timeout in sec*/
    // Assuming 'driver' is accessible and log is an existing logger object
    public static boolean waitForPageLoad(WebDriver driver1,int timeOutInSec)
    {
        try {
            WebDriverWait wait = new WebDriverWait(driver1, Duration.ofSeconds(timeOutInSec));
            // Add a custom message to the TimeoutException for better debugging
            wait.withMessage("Timeout waiting for page to load completely (readyState='complete').")
                    .until((WebDriver d) -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
            // log.info("Page loaded successfully after waiting for " + timeOutInSec + " seconds.");
            return true; // Return true on success
        } catch (TimeoutException e) {
            // Log a specific message for the expected timeout
            System.out.println("Page did not load within " + timeOutInSec + " seconds. TimeoutException: " + e.getMessage());
            return false; // Return false on failure
        } catch (Exception e) {
            // Catch any other unexpected exceptions (e.g., driver issues)
            System.out.println("An unexpected error occurred while waiting for page load: " + e.getMessage());
            return false; // Return false for unexpected errors
        }
    }

    /**
     * Fluent wait for visibility of an element, allowing custom polling.
     * Ignores {@link NoSuchElementException} during the wait period.
     *
     * @param element The WebElement to wait for visibility.
     * @param timeoutInSec The total timeout in seconds.
     * @param pollingMillis The polling interval in milliseconds.
     */
    public void fluentWaitForVisibility(WebElement element, int timeoutInSec, int pollingMillis)
    {
        new WebDriverWait(driver, Duration.ofSeconds(timeoutInSec))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.visibilityOf(element));
    }
    /**
     * Fluent wait for invisibility of an element, allowing custom polling.
     * Ignores {@link NoSuchElementException} and {@link StaleElementReferenceException}
     * during the wait period.
     *
     * @param element The WebElement to wait for invisibility.
     * @param timeoutInSec The total timeout in seconds.
     * @param pollingMillis The polling interval in milliseconds.
     */
    public void fluentWaitForInvisibility(WebElement element, int timeoutInSec, int pollingMillis) {
        new WebDriverWait(driver, Duration.ofSeconds(timeoutInSec))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.invisibilityOf(element));
    }
}
