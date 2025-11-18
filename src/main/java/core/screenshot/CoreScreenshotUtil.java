package core.screenshot;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import managers.CoreDriverManager;
import managers.CoreExtentManager;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

// Missing Javadoc for the class: CoreScreenshotUtil
/**
 * Utility class for capturing screenshots from the WebDriver instance
 * and logging them as steps in the Extent Report.
 * It uses Base64 encoding for efficient embedding of the image data.
 */
public class CoreScreenshotUtil
{
    /** Private constructor to prevent instantiation of this utility class. */
    private CoreScreenshotUtil() { /* Private constructor to prevent instantiation */ }

    /**
     * Captures a screenshot of the current browser view as a Base64 encoded String.
     * This is a low-level method used internally by {@code stepss}.
     *
     * @param driver The active WebDriver instance to take the screenshot from.
     * @return The screenshot image encoded as a Base64 String.
     */
    public static String getBase64Screenshot(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }
    /**
     * Takes a step screenshot using the current thread-local driver, logs a step
     * and attaches the image to the Extent Report.
     * This method is STATIC for easy access from any test or POM class.
     *
     * @param stepName The descriptive name for the test step.
     */
    public static void stepss(String stepName)
    {
        WebDriver driver = CoreDriverManager.getDriver();
        ExtentTest test = CoreExtentManager.getTest();
        if(test == null)
        {
            //  System.err.println("ExtentTest is null. Did you call stepss() before onTestStart?");
            return;
        }
        if(driver != null)
        {
            try
            {
                String base64Image = getBase64Screenshot(driver);
               /* // --- DEBUG STEP: Print the character length of the Base64 string ---
                int imageLength = base64Image.length();
                System.out.println("DEBUG: Base64 String Character Length for Step '" + stepName + "': " + imageLength);*/
                test.log(Status.INFO, stepName, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
            } catch (Exception e) {
                test.log(Status.INFO, stepName + " (Screenshot failed: " + e.getMessage() + ")");
                System.err.println("Failed to take step screenshot for: " + stepName + ". Error: " + e.getMessage());
            }
        } else {
            test.log(Status.INFO, stepName + " (No WebDriver instance available for screenshot)");
            System.err.println("Failed to take step screenshot (No driver): " + stepName);
        }
    }
}