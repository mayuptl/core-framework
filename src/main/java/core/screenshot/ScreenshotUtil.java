package core.screenshot;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import managers.DriverManager;
import managers.ExtentManager;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ScreenshotUtil
{
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
       WebDriver driver = DriverManager.getDriver();
       ExtentTest test = ExtentManager.getTest();
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
