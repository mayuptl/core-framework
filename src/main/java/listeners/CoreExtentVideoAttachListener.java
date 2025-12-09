package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import managers.CoreDriverManager;
import managers.CoreExtentManager;
import managers.CoreRecorderManager; // 💡 Re-import and use the thread-safe manager
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

import static core.screenshot.CoreScreenshotUtil.getBase64Screenshot;
import static core.video.CoreVideoPathUtil.toGetVideoFilePath;
/**
 * ExtentVideoAttachListeners is a streamlined listener focusing only on:
 * 1. Managing Extent Reports (start, success, failure, skip, finish).
 * 2. Starting and stopping video recording, and attaching the video link to the report.
 * 3. Attaching a screenshot to the report only on test failure.
 * This listener relies on CoreBaseTest for setting and clearing the ThreadContext.
 */
public class CoreExtentVideoAttachListener implements ITestListener {
    /**
     * Public constructor required by TestNG to instantiate this listener class
     * and hook into the test execution lifecycle events.
     */
    public CoreExtentVideoAttachListener() { }
    /** The singleton instance of the ExtentReports object for flushing results. */
    private static final ExtentReports extent = CoreExtentManager.getReportInstance();
    /**
     * Called when a test method starts.
     * <p>
     * Initializes a new thread-local ExtentTest node and starts the screen recorder
     * for the current test method.
     *
     * @param result Contains information about the test method being executed.
     */
    @Override
    public void onTestStart(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        ExtentTest classNode = CoreExtentManager.getOrCreateClassNode(className);

        String methodName = result.getMethod().getMethodName();
        ExtentTest methodNode = classNode.createNode(methodName);
        CoreExtentManager.setTest(methodNode);

        Object[] params = result.getParameters();
        if (params.length > 0) {
            methodNode.info("Parameters: " + Arrays.toString(params));
        }
        Object[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            methodNode.info("groups: " + Arrays.toString(groups));
        }
        try {
            // 💡 Call the thread-safe manager's method
            CoreRecorderManager.initializeRecorder(methodName);
            CoreRecorderManager.getRecorder().start(); // Assuming startRecording is on the instance
        } catch (Exception e) {
            System.err.println("Video recording failed to start: "+methodName);
        }
    }
    /**
     * Called when a test method succeeds.
     * <p>
     * Stops the video recorder, attaches the video link to the report, and removes the thread-local test node.
     *
     * @param result Contains information about the successfully executed test.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        /* attachScreenshot(test,driver);*/
        CoreExtentManager.removeTest();
    }
    /**
     * Called when a test method fails.
     * <p>
     * Stops the video recorder, attaches the video link, captures and attaches a screenshot,
     * logs the exception stack trace, and removes the thread-local test node.
     *
     * @param result Contains information about the failed test execution.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        attachScreenshot(test);
        test.fail(result.getThrowable());
        CoreExtentManager.removeTest();
    }
    /**
     * Called when a test method is skipped.
     * <p>
     * Stops the video recorder, attaches the video link, logs the skipped status, and removes the thread-local test node.
     *
     * @param result Contains information about the skipped test.
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        attachScreenshot(test);
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

    // --- Helper Methods ---

    /**
     * Stops the recorder, attaches the video link to the report, and cleans up Recorder ThreadLocal.
     *
     * @param test The current ExtentTest node.
     * @param methodName The name of the currently executing test method (used for video file path lookup).
     */
    private void stopAndAttachVideo(ExtentTest test, String methodName) {
        try {
            CoreRecorderManager.getRecorder().stop();
            String videoLinkHtml = toGetVideoFilePath(methodName);
            if (videoLinkHtml != null) {
                // test.info(videoLinkHtml +" : " +methodName);
                test.info("Execution video link :<br>"+videoLinkHtml);
            } else {
                test.log(Status.INFO, "Video recording file was not found after test completion.");
            }
        } catch (IllegalStateException e) {
            test.log(Status.WARNING, "Video recorder was not running for this test.");
        } catch (Exception e) {
            test.log(Status.WARNING, "Failed to stop or attach video: " + e.getMessage());
        } finally {
            CoreRecorderManager.removeInstance();
        }
    }

    /**
     * Captures a screenshot from the current WebDriver instance and adds it to the Extent Report test node.
     * This is typically only called on test failure.
     *
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