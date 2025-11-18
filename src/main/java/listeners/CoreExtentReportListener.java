package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import managers.CoreDriverManager;
import managers.CoreExtentManager;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

import static core.screenshot.CoreScreenshotUtil.getBase64Screenshot;
/**
 * Implements ITestListener to manage Extent Reports.
 * Uses ThreadLocal storage via ExtentManager to ensure parallel execution safety.
 * Assumes the Log4j ThreadContext ('driverId') is set in the TestBase class
 * during WebDriver initialization.
 */
public class CoreExtentReportListener implements ITestListener {
    /**
     * Public constructor required by TestNG to instantiate this listener class
     * and hook into the test execution lifecycle events.
     */
    public CoreExtentReportListener() { }
    /** The singleton instance of the ExtentReports object for flushing results. */
    private static ExtentReports extent = CoreExtentManager.getReportInstance();
    /**
     * Called when a test method starts.
     * <p>
     * Initializes a new thread-local ExtentTest node for the current method,
     * creates a class node if needed, and attaches method parameters and groups to the report.
     *
     * @param result Contains information about the test method being executed.
     */
    @Override
    public void onTestStart(ITestResult result) {
        // NOTE: ThreadContext.put("driverId", ...) is handled in TestBaseAppUtil,
        // ensuring the driver ID is linked to logs before the test starts.
        String className = result.getTestClass().getRealClass().getSimpleName();
        // Get or create the Class-level node (acts as the parent)
        ExtentTest classNode = CoreExtentManager.getOrCreateClassNode(className);

        String methodName = result.getMethod().getMethodName();
        // Create the Method-level node as a child of the class node
        ExtentTest methodNode = classNode.createNode(methodName);
        // Store the current test node in ThreadLocal storage
        CoreExtentManager.setTest(methodNode);

        Object[] params = result.getParameters();
        if (params.length > 0) {
            methodNode.info("Parameters: " + Arrays.toString(params));
        }
        Object[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            methodNode.info("groups: " + Arrays.toString(groups));
        }
    }

    /**
     * Called when a test method succeeds.
     * <p>
     * Removes the thread-local test node to prevent memory leaks and thread confusion.
     *
     * @param result Contains information about the successfully executed test.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        /*ExtentTest test = ExtentManager.getTest();
         attachScreenshot(test,driver);*/
        CoreExtentManager.removeTest();
    }

    /**
     * Called when a test method fails.
     * <p>
     * Captures a screenshot, attaches it to the report, logs the exception stack trace,
     * and removes the thread-local test node.
     *
     * @param result Contains information about the failed test execution.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        attachScreenshot(test);
        test.fail(result.getThrowable());
        CoreExtentManager.removeTest();
    }
    /**
     * Called when a test method is skipped.
     * <p>
     * Logs the skipped status along with the throwable (if present) and removes the thread-local test node.
     *
     * @param result Contains information about the skipped test.
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        test.skip("Test Skipped: " + result.getThrowable());
        CoreExtentManager.removeTest();
    }

    /**
     * Called when all the tests belonging to the test tag in the XML file have run.
     * <p>
     * Flushes the ExtentReports object to write the collected test data to the output file(s).
     *
     * @param context The test context (suite or test tag) that has finished execution.
     */
    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }
    /**
     * Captures a screenshot and adds it to the Extent Report test node.
     * Uses addScreenCaptureFromBase64String() to attach the image.
     * @param test The current ExtentTest node.
     */
    private void attachScreenshot(ExtentTest test)
    {
        WebDriver driver = CoreDriverManager.getDriver();
        if (driver != null) {
            String base64Screenshot = getBase64Screenshot(driver);
            //test.log(Status.INFO,stepName, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
            test.addScreenCaptureFromBase64String(base64Screenshot);
        }else {
            System.err.println("Driver is null. failed to attached screenshot");
        }
    }
    // Unused methods required by ITestListener interface
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onTestFailedWithTimeout(ITestResult result) {}
}