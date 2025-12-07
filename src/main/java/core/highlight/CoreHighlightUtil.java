package core.highlight;
import managers.CoreDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.sql.DriverManager;

import static core.config.CoreConfigReader.getIntProp;
import static core.config.CoreConfigReader.getStrProp;

/**
 * Utility class for performing standard Selenium actions while applying a
 * **persistent visual highlight (border)** to the target element.
 *
 * <p>This class is primarily used for debugging and demonstration purposes in test automation.
 * The highlight remains on the element until a page navigation occurs (DOM reset).
 * All public methods are designed to safely catch and handle exceptions (like {@code StaleElementReferenceException}
 * or operation failures) without stopping test execution, providing a safe, robust wrapper around core {@link WebElement} operations.</p>
 */
public class CoreHighlightUtil {

    //private final WebDriver driver;
    /** Private constructor for a utility class; all methods are static. */
    private CoreHighlightUtil() { }
    /**
     * Applies a dashed border highlight and ensures the element is visible by scrolling it into view.
     *
     * <p>This is a private helper method that uses {@link JavascriptExecutor} to modify the element's style.
     * Exceptions during highlighting (e.g., if the element is stale or non-existent) are caught and logged
     * to prevent execution failure in the calling method.</p>
     *
     * @param element The {@link WebElement} to highlight.
     * @param initialStyle The initial border color string
     * @param finalStyle The final border color string
     */
//    private static void applyHighlight(WebElement element, String color) {
//        WebDriver driver = CoreDriverManager.getDriver();
//        try {
//            scrollTo(element);
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            // Script for highlight: 3px dashed border of the specified color.
//            String script = "arguments[0].style.border='3px dashed "+color+"'";
//            js.executeScript(script, element);
//        } catch (Exception e) {
//            System.err.println("Highlighting failed for element: " + element + ". Error: " + e.getMessage());
//            // Execution continues even if highlighting fails.
//        }
//    }

    private static void applyHighlight(WebElement element, String initialStyle, String finalStyle)
    {
        // The driver, properties, and color extraction logic are correct.
        WebDriver driver = CoreDriverManager.getDriver();
//        int changeMillis =Integer.parseInt(getStrProp("STYLE_CHANGE_IN_MILLIS"));
//        int cleanupDelayMs = Integer.parseInt(getStrProp("CLEANUP_DELAY_IN_MILLIS"));
        int changeMillis =getIntProp("STYLE_CHANGE_IN_MILLIS");
        int cleanupDelayMs = getIntProp("CLEANUP_DELAY_IN_MILLIS");
        //"        elem.style.boxShadow = '0 0 10px #FFD700';" +
        String shadowColor = "#FF0000"; // Default color in case initialStyle is malformed
        // --- Color Extraction Logic (Corrected for robustness) ---
        // Ensure initialStyle is not null and is processed.
        if (initialStyle != null && !initialStyle.trim().isEmpty()) {
            String[] parts = initialStyle.trim().split("\\s+");
            // We assume the format has at least three parts: width, style, and color.
            if (parts.length >= 3) {
                // The color is always the last element in the resulting array.
                shadowColor = parts[parts.length - 1];
            }
        }
        try {
          //  System.out.println(shadowColor);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                    "var elem = arguments[0];" +
                    "var initStyle = arguments[1];" +
                    "var finalStyle = arguments[2];" +
                    "var changeDelay = arguments[3];" +
                    "var cleanupDelay = arguments[4];" +
                    "try {" +
                    "    if (elem && elem.style){" +
                    "        elem.style.border = initStyle;" +
                    "        elem.style.boxShadow = '0 0 10px "+ shadowColor +"';" +
                    "    }" +
                    "    setTimeout(function() {" +
                    "       try {" +
                    "           if(elem && elem.style) {" +
                    "               elem.style.border = finalStyle;" +
                    "               elem.style.boxShadow = 'none';" +
                    "           }" +
                    "       } catch(e) {  }" + //console.warn('Error applying final style', e);
                    "    }, changeDelay);" +
                    "    setTimeout(function() {" +
                    "       try{" +
                    "           if(elem && elem.style) {" +
                    "               elem.style.border = 'none';" +
                    "               elem.style.boxShadow = 'none';" +
                    "           }" +
                    "       } catch(e) {  }" + //console.warn('Error during cleanup', e);
                    "    }, cleanupDelay);" +

                            "}  catch(e) {  }"; //console.error('Highlight script failed', e);

            js.executeScript(script, element, initialStyle, finalStyle, changeMillis, cleanupDelayMs);

        } catch (Exception e)
        {
            //System.err.println("Highlight failed for element: " + element +". Error: " + e.getMessage());
        }
    }
    /**
     * Clicks the specified element after applying a green highlight.
     *
     * <p>The method wraps the {@code click()} operation, catches all exceptions (including {@code StaleElementReferenceException} and {@code ElementClickInterceptedException}), and logs the error, ensuring the test execution does not stop.</p>
     *
     * @param element The {@link WebElement} to be clicked.
     */
    public static void click(WebElement element) {
        try {
            applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
            element.click();
        } catch (Exception e) {
            System.err.println("Click failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Clears the input field and then enters the specified text after applying a green highlight.
     *
     * <p>The method catches all exceptions and logs the error, ensuring the test execution does not stop.</p>
     *
     * @param element The input {@link WebElement} to clear and send keys to.
     * @param value The text to be entered into the input field.
     */
    public static void sendKeys(WebElement element, String value) {
        try {
            applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
            element.clear();
            element.sendKeys(value);
        } catch (Exception e) {
            System.err.println("SendKeys failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Appends text to the existing content of an input field after applying a green highlight.
     * This method skips the {@code clear()} operation.
     *
     * <p>The method catches all exceptions and logs the error, ensuring the test execution does not stop.</p>
     *
     * @param element The input {@link WebElement} to append keys to.
     * @param value The text to be appended.
     */
    public static void sendKeysAppend(WebElement element, String value) {
        try {
            applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
            element.sendKeys(value);
        } catch (Exception e) {
            System.err.println("SendKeysAppend failed for element: " + element + ". Error: " + e.getMessage());
        }
    }
    /**
     * Retrieves the visible text of an element after applying a green highlight.
     *
     * <p>The method catches all exceptions and returns an empty string, ensuring the test execution does not stop.</p>
     *
     * @param element The {@link WebElement} from which to retrieve the text.
     * @return The visible text of the element, or an empty string if the operation fails.
     */
    public static String getText(WebElement element) {
        try {
            applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
            return element.getText();
        } catch (Exception e) {
            System.err.println("GetText failed for element: " + element + ". Error: " + e.getMessage());
            return ""; // Safe return value
        }
    }
    /**
     * Compares the actual text of an element with an expected value, applying
     * visual feedback based on the comparison result.
     * <ul>
     * <li>Applies a **green** border on match.</li>
     * <li>Applies a **red** border on mismatch.</li>
     * </ul>
     *
     * <p>The method catches all exceptions and returns {@code false}, ensuring the test execution does not stop.</p>
     *
     * @param element The {@link WebElement} whose text will be compared.
     * @param expectedText The expected text value.
     * @return {@code true} if the texts match, {@code false} otherwise or if the operation fails.
     */
    public static boolean compareText(WebElement element, String expectedText) {
        try {
            String actualText = element.getText();
            boolean match = expectedText.equals(actualText);
            if(match)
            {
                applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
                return match;
            }else {
                applyHighlight(element, getStrProp("RED_BORDER_STYLE"),getStrProp("RED_BORDER_STYLE"));
                return match;
            }
        } catch (Exception e) {
            System.err.println("CompareText failed for element: " + element + ". Error: " + e.getMessage());
            return false; // Safe return value
        }
    }
    /**
     * Checks if an element is currently displayed on the page, applying a green highlight if visible.
     *
     * <p>The method catches all exceptions (including {@code NoSuchElementException} and {@code StaleElementReferenceException})
     * and returns {@code false}, ensuring the test execution does not stop. This is a thread-safe way to verify element presence.</p>
     *
     * @param element The {@link WebElement} to check.
     * @return {@code true} if the element is displayed, {@code false} if not displayed or the check fails due to an exception.
     */
    public static boolean isDisplayed(WebElement element) {
        try {
            boolean displayed = element.isDisplayed();
            if (displayed) applyHighlight(element, getStrProp("INIT_BORDER_STYLE"),getStrProp("FINAL_BORDER_STYLE"));
            return displayed;
        } catch (Exception e) {
            System.err.println("IsDisplayed check failed for element: " + element + ". Error: " + e.getMessage());
            // Catching all exceptions and returning false ensures test continues, as requested.
            return false;
        }
    }
    /**
     * Scrolls the element into the viewport using JavaScript's {@code scrollIntoView} method.
     *
     * <p>It first checks if the element is already in view before attempting a smooth scroll to the center of the viewport.
     * Exceptions during scrolling are caught and ignored.</p>
     *
     * @param element The {@link WebElement} to scroll to.
     */
    public static void scrollTo(WebElement element) {
        WebDriver driver = CoreDriverManager.getDriver();
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
