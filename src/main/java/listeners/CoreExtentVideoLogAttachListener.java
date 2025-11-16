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

// Assuming GetVideoFilePath.toGetVideoFilePath is available
// import static core.video.GetVideoFilePath.toGetVideoFilePath;

/**
 * ExtentVideoLogAttachListeners is the comprehensive listener responsible for:
 * 1. Managing Extent Reports (start, success, failure, skip, finish).
 * 2. Starting and stopping video recording, and attaching the video link to the report.
 * 3. Attaching logs retrieved via LogExtractorUtil to every test case (pass/fail).
 * 4. Attaching a screenshot to the report only on test failure.
 *
 * It relies on TestBaseApputil for setting and clearing the ThreadContext (driverId, testName).
 */
public class CoreExtentVideoLogAttachListener implements ITestListener {

    private static final ExtentReports extent = CoreExtentManager.getReportInstance();
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
    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        /* attachScreenshot(test,driver);*/
        attachLogs(test,methodName,result);
        CoreExtentManager.removeTest();
    }

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

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CoreExtentManager.getTest();
        String methodName = result.getMethod().getMethodName();
        stopAndAttachVideo(test, methodName);
        test.skip("Test Skipped: " + result.getThrowable());
        CoreExtentManager.removeTest();
    }

    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }

    private String getDriverIdFromContext() {
        return ThreadContext.get("driverId");
    }
    private void attachLogs(ExtentTest test,String methodName,ITestResult result)  {
        String driverID = getDriverIdFromContext();
        String testLogs= CoreLogFilter.toGetTestCaseLogs(methodName,driverID);
        String styledLogs=
                "<div style='overflow-x:auto;'><pre style='white-space: pre-wrap; word-break: break-word;'>"
                        + testLogs + "</pre></div>";
        test.info(styledLogs);
        createClassLevelLogsFolder(testLogs,result);
    }
    public void createClassLevelLogsFolder(String testLogs,ITestResult result)
    {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();
        String logDir=getStrProp("LOG_OUTPUT_DIR"); //execution-output/test-logs
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
    /** Stops the recorder, attaches the video link to the report, and cleans up Recorder ThreadLocal. */
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