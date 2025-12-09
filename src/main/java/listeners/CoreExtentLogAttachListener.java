package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import core.logging.CoreLogFilter;
import managers.CoreDriverManager;
import managers.CoreExtentManager;
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

/**
 * ExtentLogAttachListeners is the comprehensive listener responsible for:
 * 1. Managing Extent Reports (start, success, failure, skip, finish).
 * 2. Attaching logs retrieved via CoreLogFilter to every test case (pass/fail).
 * 3. Attaching a screenshot to the report only on test failure.
 * 4. Ensuring thread-safe reporting via ExtentManager and ThreadLocal.
 */
public class CoreExtentLogAttachListener implements ITestListener {
    /**
     * Public constructor required by TestNG to instantiate this listener class
     * and hook into the test execution lifecycle events.
     */
    public CoreExtentLogAttachListener() { }
    /** The singleton instance of the ExtentReports object for flushing results. */
    private static final ExtentReports extent = CoreExtentManager.getReportInstance();

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
    }

    /**
     * Called when a test method succeeds.
     * <p>
     * Attaches segmented log content to the report and removes the thread-local test node.
     *
     * @param result Contains information about the successfully executed test.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        /*attachScreenshot(test);*/
        attachLogs(test,methodName,result);
        CoreExtentManager.removeTest();
    }

    /**
     * Called when a test method fails.
     * <p>
     * Captures a screenshot, attaches it to the report, extracts and attaches logs,
     * logs the exception stack trace, and removes the thread-local test node.
     *
     * @param result Contains information about the failed test execution.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        attachScreenshot(test);
        attachLogs(test,methodName,result);
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
     * Retrieves the driver ID from the Log4j2 ThreadContext.
     * @return The thread-specific driver ID.
     */
    private String getDriverIdFromContext() {
        return ThreadContext.get("driverId");
    }
    /**
     * Captures a screenshot and adds it to the Extent Report test node.
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
    /**
     * Extracts test case logs and attaches them to the Extent Report as formatted HTML.
     * It also saves the logs to a dedicated file under the class-level log folder.
     *
     * @param test The current ExtentTest node.
     * @param methodName The name of the currently executing test method.
     * @param result The ITestResult to extract class and method names for file saving.
     */
    private void attachLogs(ExtentTest test,String methodName,ITestResult result)  {
        String driverID = getDriverIdFromContext();
        String testLogs= CoreLogFilter.toGetTestCaseLogs(methodName,driverID);
        String styledLogs=
                "<div style='overflow-x:auto;'><pre style='white-space: pre-wrap; word-break: break-word;'>"
                        + testLogs + "</pre></div>";
        // test.info(styledLogs);
        test.info(MarkupHelper.createCodeBlock(testLogs).getMarkup());
        createClassLevelLogsFolder(testLogs,result);
    }
    /**
     * Creates a dedicated folder for the current test class and writes the extracted test logs
     * to a file named after the test method. This provides persistent access to test logs.
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
    // Unused methods required by ITestListener interface
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onTestFailedWithTimeout(ITestResult result) {}
}