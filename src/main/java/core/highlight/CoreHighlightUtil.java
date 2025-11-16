package core.highlight;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Utility class for performing standard Selenium actions while applying a
 * persistent visual highlight (border) to the target element.
 * <p>The highlight remains on the element until a page navigation occurs (DOM reset).
 * All methods are designed to safely catch and handle exceptions (like {@code StaleElementReferenceException}
 * or operation failures) without stopping test execution.</p>
 */
public class CoreHighlightUtil {

    private final WebDriver driver;
    /**
     * Initializes the Highlight utility with the WebDriver instance.
     *
     * @param driver The active WebDriver instance.
     */
    public CoreHighlightUtil(WebDriver driver) {
        this.driver = driver;
    }
    /**
     * Applies a dashed border highlight and scrolls the element into view.
     * Exceptions are caught and ignored to prevent execution failure.
     *
     * @param element The {@link WebElement} to highlight.
     * @param color The border color (e.g., "green", "red").
     */
    private void applyHighlight(WebElement element, String color) {
        try {
            scrollTo(element);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Script for highlight: 3px dashed border of the specified color.
            String script = "arguments[0].style.border='3px dashed "+color+"'";
            js.executeScript(script, element);
        } catch (Exception e) {
            System.err.println("Highlighting failed for element: " + element + ". Error: " + e.getMessage());
            // Execution continues even if highlighting fails.
        }
    }
    /**
     * Clicks the specified element after applying a green highlight.
     * The method catches all exceptions and logs the error, ensuring the test execution does not stop.
     *
     * @param element The {@link WebElement} to be clicked.
     */
    public void click(WebElement element) {
        try {
            applyHighlight(element, "green");
            element.click();
        } catch (Exception e) {
            System.err.println("Click failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Clears the element and then enters the specified text after applying a green highlight.
     * The method catches all exceptions and logs the error, ensuring the test execution does not stop.
     *
     * @param element The input {@link WebElement} to clear and send keys to.
     * @param value The text to be entered into the input field.
     */
    public void sendKeys(WebElement element, String value) {
        try {
            applyHighlight(element, "green");
            element.clear();
            element.sendKeys(value);
        } catch (Exception e) {
            System.err.println("SendKeys failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Appends text to the existing text in an input field after applying a green highlight.
     * This method does not clear the input field first.
     * The method catches all exceptions and logs the error, ensuring the test execution does not stop.
     *
     * @param element The input {@link WebElement} to append keys to.
     * @param value The text to be appended.
     */
    public void sendKeysAppend(WebElement element, String value) {
        try {
            applyHighlight(element, "green");
            element.sendKeys(value);
        } catch (Exception e) {
            System.err.println("SendKeysAppend failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Retrieves the visible text of an element after applying a green highlight.
     * The method catches all exceptions and returns an empty string, ensuring the test execution does not stop.
     *
     * @param element The {@link WebElement} from which to retrieve the text.
     * @return The visible text of the element, or an empty string if the operation fails.
     */
    public String getText(WebElement element) {
        try {
            applyHighlight(element, "green");
            return element.getText();
        } catch (Exception e) {
            System.err.println("GetText failed for element: " + element + ". Error: " + e.getMessage());
            return ""; // Safe return value
        }
    }
    /**
     * Compares the actual text of an element with an expected value, applying
     * visual feedback based on the result.
     * <ul>
     * <li>Applies a 'green' border on match.</li>
     * <li>Applies a 'red' border on mismatch.</li>
     * </ul>
     * The method catches all exceptions and returns {@code false}, ensuring the test execution does not stop.
     *
     * @param element The {@link WebElement} whose text will be compared.
     * @param expectedText The expected text value.
     * @return true if the texts match, false otherwise or if the operation fails.
     */
    public boolean compareText(WebElement element, String expectedText) {
        try {
            String actualText = element.getText();
            boolean match = expectedText.equals(actualText);
            applyHighlight(element, match ? "green" : "red");
            return match;
        } catch (Exception e) {
            System.err.println("CompareText failed for element: " + element + ". Error: " + e.getMessage());
            return false; // Safe return value
        }
    }
    /**
     * Checks if an element is currently displayed on the page.
     * The method catches all exceptions (including {@code NoSuchElementException} and {@code StaleElementReferenceException})
     * and returns {@code false}, ensuring the test execution does not stop.
     *
     * @param element The {@link WebElement} to check.
     * @return true if the element is displayed, false if not displayed or the check fails.
     */
    public boolean isDisplayed(WebElement element) {
        try {
            boolean displayed = element.isDisplayed();
            if (displayed) applyHighlight(element, "green");
            return displayed;
        } catch (Exception e) {
            System.err.println("IsDisplayed check failed for element: " + element + ". Error: " + e.getMessage());
            // Catching all exceptions and returning false ensures test continues, as requested.
            return false;
        }
    }
    /**
     * Scrolls the element into the viewport only if it is not currently in view.
     * Uses smooth scrolling behavior to center the element.
     * Exceptions are caught and ignored to prevent execution failure.
     *
     * @param element The {@link WebElement} to scroll to.
     */
    public void scrollTo(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Check if the element is in the viewport
            boolean isElementInView = (boolean) js.executeScript(
                    "var rect = arguments[0].getBoundingClientRect();" +
                            "return (rect.top >= 0 && rect.bottom <= window.innerHeight);", element);
            // If not in viewport, scroll to the element
            if (!isElementInView) {
                js.executeScript("arguments[0].scrollIntoView({ behavior: 'smooth', block: 'center' });", element);
            }
        } catch (Exception e) {
            System.err.println("Scrolling failed for element: " + element + ". Error: " + e.getMessage());
            // Execution continues even if scrolling fails.
        }
    }
//========================================================================================//
    // Script for blink effect
        /*  js.executeScript(
                "arguments[0].style.border='2px dashed ' + arguments[1];" +
                        "arguments[0].style.animation = 'blink 0.3s 3';" +
                        "var style= document.createElement('style');" +
                        "style.innerHTML = '@keyframes blink { 0% { border-color: transparent; } 50% { border-color: ' + arguments[1] +'; } 100% { border-color: transparent; }}';" +
                        "document.head.appendChild(style);", element, color);
        */

/*
    private void removeBorder(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String script = "arguments[0].style.border='';";
        js.executeScript(script, element);
    }

    private void applyBg(WebElement element, String color) {
        scrollTo(element);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Script for highlight
        js.executeScript("arguments[0].style.backgroundColor = arguments[1];", element, color);
        // Script for blink effect
        *//* js.executeScript("arguments[0].style.backgroundColor = arguments[1];" +
                "arguments[0].style.animation = 'blink 0.1s 3';" +
                "var style= document.createElement('style');" +
                "style.innerHTML = '@keyframes blink { 0% { border-color: transparent; } 50% { border-color: ' + arguments[1] +'; } 100% { border-color: transparent; }}';" +
                "document.head.appendChild(style);", element, color);
        *//*
    }*/
}
