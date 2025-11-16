package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import core.logging.LogExtractorUtil;
import managers.DriverManager;
import managers.ExtentManager;
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

import static core.config.ConfigReader.getStrProp;
import static core.screenshot.ScreenshotUtil.getBase64Screenshot;
import static java.nio.file.Files.createDirectory;

/**
 * ExtentLogAttachListeners is the comprehensive listener responsible for:
 * 1. Managing Extent Reports (start, success, failure, skip, finish).
 * 2. Attaching logs retrieved via LogExtractorUtil to every test case (pass/fail).
 * 3. Attaching a screenshot to the report only on test failure.
 * 4. Ensuring thread-safe reporting via ExtentManager and ThreadLocal.
 */
public class ExtentLogAttachListeners implements ITestListener {
    private static final ExtentReports extent = ExtentManager.getReportInstance();

    @Override
    public void onTestStart(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        ExtentTest classNode = ExtentManager.getOrCreateClassNode(className);
        String methodName = result.getMethod().getMethodName();
        ExtentTest methodNode = classNode.createNode(methodName);
        ExtentManager.setTest(methodNode);
        Object[] params = result.getParameters();
        if (params.length > 0) {
            methodNode.info("Parameters: " + Arrays.toString(params));
        }
        Object[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            methodNode.info("groups: " + Arrays.toString(groups));
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        /*attachScreenshot(test;*/
        attachLogs(test,methodName,result);
        ExtentManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        attachScreenshot(test);
        attachLogs(test,methodName,result);
        test.fail(result.getThrowable());
        ExtentManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        test.skip("Test Skipped: " + result.getThrowable());
        ExtentManager.removeTest();
    }

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
        WebDriver driver = DriverManager.getDriver();
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
     * (NOTE: LogExtractorUtil.toGetTestCaseLogs() must be implemented elsewhere)
     * @param test The current ExtentTest node.
     * @param methodName The name of the currently executing test method.
     */
    private void attachLogs(ExtentTest test,String methodName,ITestResult result)  {
        String driverID = getDriverIdFromContext();
        String testLogs=LogExtractorUtil.toGetTestCaseLogs(methodName,driverID);
        String styledLogs=
                "<div style='overflow-x:auto;'><pre style='white-space: pre-wrap; word-break: break-word;'>"
                        + testLogs + "</pre></div>";
       // test.info(styledLogs);
        test.info(MarkupHelper.createCodeBlock(testLogs).getMarkup());
        createClassLevelLogsFolder(testLogs,result);
    }
    public void createClassLevelLogsFolder(String testLogs,ITestResult result)
    {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();
        String logDir=getStrProp("LOG_FILE_DIR"); //execution-output/test-logs
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