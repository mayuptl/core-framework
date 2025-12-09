package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import core.logging.CoreLogFilter;
import managers.CoreDriverManager;
import managers.CoreExtentManager;
import managers.CoreRecorderManager;
import org.apache.logging.log4j.ThreadContext;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static core.config.CoreConfigReader.getStrProp;
import static core.screenshot.CoreScreenshotUtil.getBase64Screenshot;
import static core.video.CoreVideoPathUtil.toGetVideoFilePath;
/**
 * ExtentVideoLogAttachListeners is the comprehensive listener responsible for:
 * 1. Managing Extent Reports (start, success, failure, skip, finish).
 * 2. Starting and stopping video recording, and attaching the video link to the report.
 * 3. Attaching logs retrieved via CoreLogFilter to every test case (pass/fail).
 * 4. Attaching a screenshot to the report only on test failure.
 * It relies on CoreBaseTest for setting and clearing the ThreadContext (driverId, testName).
 */
public class CoreExtentVideoLogAttachListener implements ITestListener {
    /**
     * Public constructor required by TestNG to instantiate this listener class
     * and hook into the test execution lifecycle events.
     */
    public CoreExtentVideoLogAttachListener() { }
    /** The singleton instance of the ExtentReports object for flushing results. */
    private static final ExtentReports extent = CoreExtentManager.getReportInstance();

    /**
     * Called when a test method starts.
     * <p>
     * Initializes a new thread-local ExtentTest node, attaches test parameters,
     * and initializes and starts the screen recorder for the current test method.
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
            CoreRecorderManager.initializeRecorder(methodName);
            CoreRecorderManager.getRecorder().start();
        } catch (Exception e) {
            System.err.println("Video recording failed to start: "+methodName);
        }
    }
    /**
     * Called when a test method succeeds.
     * <p>
     * Stops video recording, attaches the video link and logs to the report, and cleans up thread-local storage.
     *
     * @param result Contains information about the successfully executed test.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        /* attachScreenshot(test,driver);*/
        attachLogs(test,methodName,result);
        CoreExtentManager.removeTest();
    }
    /**
     * Called when a test method fails.
     * <p>
     * Stops video recording, attaches video, captures and attaches a screenshot,
     * attaches logs, and logs the exception stack trace before cleanup.
     *
     * @param result Contains information about the failed test execution.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        attachScreenshot(test);
        attachLogs(test,methodName,result);
        test.fail(result.getThrowable());
        CoreExtentManager.removeTest(); // ThreadLocal cleanup
    }
    /**
     * Called when a test method is skipped.
     * <p>
     * Stops video recording, attaches video, logs the skipped status, and cleans up thread-local storage.
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
    /**
     * Retrieves the driver ID from the Log4j2 ThreadContext, which links the current thread
     * to its specific log file segment.
     * @return The thread-specific driver ID, or null if not set.
     */
    private String getDriverIdFromContext() {
        return ThreadContext.get("driverId");
    }
    /**
     * Extracts test case logs from the dedicated log file segment and attaches them
     * to the Extent Report as formatted HTML (pre-wrapped text).
     * This method also calls {@link #createClassLevelLogsFolder(String, ITestResult)} to save logs persistently.
     * * @param test The current ExtentTest node.
     * @param methodName The name of the currently executing test method.
     * @param result The ITestResult to extract class and method names for file saving.
     */
    private void attachLogs(ExtentTest test,String methodName,ITestResult result)  {
        String driverID = getDriverIdFromContext();
        String testLogs= CoreLogFilter.toGetTestCaseLogs(methodName,driverID);
        String styledLogs=
                "<div style='overflow-x:auto;'><pre style='white-space: pre-wrap; word-break: break-word;'>"
                        + testLogs + "</pre></div>";
        test.info(styledLogs);
        createClassLevelLogsFolder(testLogs,result);
    }
    /**
     * Creates a dedicated folder structure based on the class name and writes the raw test logs
     * to a file named after the test method (e.g., {@code testName.logs}).
     * This provides persistent, unformatted log access outside the report.
     *
     * @param testLogs The log content extracted for the current test method.
     * @param result The ITestResult object containing class and method names.
     */
    public void createClassLevelLogsFolder(String testLogs,ITestResult result)
    {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();
        String logDir=getStrProp("log.output.dir"); //execution-output/test-logs
        String folderPathString = logDir +"/class-level-logs"; //execution-output/test-logs/class-level-logs
        String relativePath = folderPathString +"/"+ className; //execution-output/test-logs/classLevelLogs/className

        Path targetPath = Paths.get(relativePath);
        Path methodLevelogFilePath = targetPath.resolve(methodName + ".logs");
        try {
            Files.createDirectories(targetPath);
            Files.writeString(
                    methodLevelogFilePath,          // The file path object
                    testLogs,             // The string content
                    StandardCharsets.UTF_8 // Ensure consistent encoding
            );
        } catch (IOException e) {
            System.err.println("Failed to create directories due to an I/O error: " + e.getMessage());
        }
    }
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
                //  test.info(videoLinkHtml +" : " +methodName);
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