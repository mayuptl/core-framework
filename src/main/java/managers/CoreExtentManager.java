package managers;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static core.config.CoreConfigReader.getStrProp;
/**
 * Manages the ExtentReports instance and provides thread-safe access to the
 * ExtentTest object for the currently executing test method.
 *
 * This manager ensures that reporting is properly initialized and handles the
 * creation of class-level nodes and method-level test instances.
 */
public class CoreExtentManager {
    private static ExtentReports extent;
    private static final String DEFAULT_REPORT_PATH = getStrProp("REPORT_OUTPUT_DIR")+getStrProp("REPORT_NAME");
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();
    private static final Map<String,ExtentTest> classNodeMap = new ConcurrentHashMap<>();
    /**
     * <b>Initializes or returns the singleton ExtentReports instance using a custom file path.</b>
     * This method configures the report's theme, title, and system information.
     *
     * @param reportFilePath The path where the Extent Report HTML file should be saved.
     * @return The initialized ExtentReports instance.
     */
    public static ExtentReports getReportInstance(String reportFilePath) {
        if (extent == null) {
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportFilePath);
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle(getStrProp("EXTENT_DOCUMENT_TITLE","Test Automation Report"));
            sparkReporter.config().setReportName(getStrProp("EXTENT_REPORT_NAME","Test Results"));
            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);
            extent.setSystemInfo("REPORT_SCOPE", getStrProp("REPORT_SCOPE","Sprint testing"));
            extent.setSystemInfo("ENVIRONMENT", getStrProp("ENVIRONMENT","Test"));
            extent.setSystemInfo("APPLICATION_VERSION", getStrProp("APPLICATION_VERSION","-"));
            extent.setSystemInfo("TESTER_NAME",getStrProp("TESTER_NAME","QA"));
            //   extent.setSystemInfo("OS", System.getProperty("os.name"));
            //  extent.setSystemInfo("JAVA_VERSION", System.getProperty("java.version"));
        }
        return extent;
    }

    /**
     * <b>Initializes or returns the singleton ExtentReports instance using the default path:</b>
     * `execution-output/test-reports/ExtentReport.html`
     *
     * @return The initialized ExtentReports instance.
     */
    public static ExtentReports getReportInstance() {
        return getReportInstance(DEFAULT_REPORT_PATH);
    }
    /**
     * Retrieves an existing parent ExtentTest node for a given class name, or creates a new one
     * if it doesn't exist. This ensures test methods are correctly grouped under their respective class.
     *
     * @param clasName The simple name of the test class (e.g., "LoginTest").
     * @return The class-level ExtentTest node.
     */
    public static ExtentTest getOrCreateClassNode(String clasName)
    {
        // Ensure the report is initialized before creating nodes
        getReportInstance();
        return classNodeMap.computeIfAbsent(clasName,k->extent.createTest(k));
    }
    /**
     * Returns the **thread-local** ExtentTest instance associated with the current running test method.
     *
     * @return The ExtentTest instance for the current thread, or null if not set.
     */
    public static ExtentTest getTest() {
        return currentTest.get();
    }
    /**
     * Sets the current **thread-local** ExtentTest instance. This is typically called by the Test Listener (onTestStart)
     * to associate a test method with a specific report entry.
     *
     * @param test The ExtentTest instance for the current method.
     */
    public static void setTest(ExtentTest test) {
        currentTest.set(test);
    }
    /**
     * Removes the ExtentTest instance from the current thread's storage. This prevents memory leaks
     * and is called by the Test Listener (onTestSuccess/Failure/Skip) when the test completes.
     */
    public static void removeTest() {
        currentTest.remove();
    }

    //==========================================================//
    /**
     * Writes all buffered information to the physical report file.
     * This must be called once after all tests have completed.
     */
   /* public static void flushReport() {
        if (extent != null) {
            extent.flush();
            System.out.println("Extent Report successfully generated.");
        }
    }*/
    /**
     * Creates a new ExtentTest instance for a test method and stores it in the thread-local storage.
     *
     * @param testName The name of the test method.
     * @param description A brief description of the test.
     */
  /* public static void createTest(String testName, String description) {
        // Find the class node or create it
        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        ExtentTest classNode = getOrCreateClassNode(className);

        // Create the test method node as a child of the class node
        ExtentTest test = classNode.createNode(testName, description);
        setTest(test);
    }*/

    /**
     * Returns the file path used to generate the Extent Report.
     *
     * @return The report file path string.
     */
   /* public static String getReportFilePath() {
        return DEFAULT_REPORT_PATH;
    }*/

    /**
     * Initializes the Extent Report, including configuration and system info.
     */
   /* public static void initReport() {
        getReportInstance();
    }*/
}