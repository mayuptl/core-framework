package core.logging;

import core.config.CoreConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static core.config.CoreConfigReader.getIntProp;
import static core.config.CoreConfigReader.getStrProp;
/**
 * Utility class for extracting specific log segments corresponding to a single test case
 * and driver session from a log file, using configurable start and end markers.
 * This class relies on properties defined in the configuration files (via {@code ConfigReader}).
 */
public class CoreLogFilter {
    /** Private constructor for a utility class; all methods are static. */
    private CoreLogFilter() { }
     /** Directory where log files are stored, configured by "LOG_OUTPUT_DIR". */
    static String logDir = CoreConfigReader.getStrProp("LOG_OUTPUT_DIR");
    /** The base name of the log file, configured by "LOG_FILE_NAME". */
    static String logFileName= CoreConfigReader.getStrProp("LOG_FILE_NAME");
/** The default full path to the log file, constructed from logDir and logFileName. */
    private static final String DEFAULT_LOG_PATH = logDir + logFileName;

    /**
     * Extracts logs using a test case name, a driver ID, and the default log file path:
     * {@code ${user.dir}/execution-output/test-logs/Logs.log}.
     *
     * @param testCaseName The test method name (e.g., "ToCheckLogin") used to identify log segment start/end points.
     * @param driverID     The unique identifier for the WebDriver session (e.g., a thread ID or hash).
     * @return The extracted logs as a single String, or an error message if the file is not found or cannot be read.
     */
    public static String toGetTestCaseLogs(String testCaseName, String driverID) {
        return toGetTestCaseLogsCoreLogic(testCaseName, driverID, DEFAULT_LOG_PATH);
    }

    /**
     * Extracts logs using a test case name, a driver ID, and a custom log file path.
     *
     * @param testCaseName The test method name (e.g., "ToCheckLogin") used to identify log segment start/end points.
     * @param driverID     The unique identifier for the WebDriver session (e.g., a thread ID or hash).
     * @param customPath   The user-provided path to the log file.
     * @return The extracted logs as a single String, or an error message if the file is not found or cannot be read.
     */
    public static String toGetTestCaseLogs(String testCaseName, String driverID, String customPath) {
        String finalPath = (customPath != null && !customPath.trim().isEmpty())
                ? customPath
                : DEFAULT_LOG_PATH;
        return toGetTestCaseLogsCoreLogic(testCaseName, driverID, finalPath);
    }
    /**
     * Core logic for reading the log file, identifying the test case log segment, and extracting lines.
     * <p>Extraction criteria:</p>
     * <ul>
     * <li>Starts when a line matches all three markers: {@code START_MARKER}, {@code testCaseName}, and {@code driverID}.</li>
     * <li>Stops when a line matches either the {@code END_PASS_MARKER} or {@code END_FAIL_MARKER}.</li>
     * <li>Only lines containing the {@code driverID} or {@code testCaseName} are captured between markers.</li>
     * <li>A maximum capture limit (MAX_CAPTURE_LINES) is enforced to prevent large memory consumption.</li>
     * </ul>
     *
     * @param testCaseName The name of the test case to look for.
     * @param driverID The driver session ID to thread the logs.
     * @param filePath The resolved path to the log file.
     * @return The extracted log segment.
     */
    private static String toGetTestCaseLogsCoreLogic(String testCaseName, String driverID, String filePath) {
        if (testCaseName == null || testCaseName.trim().isEmpty()) {
            return "ERROR: Cannot extract logs. The testCaseName provided is null or empty.";
        }
        if (driverID == null || driverID.trim().isEmpty()) {
            return "ERROR: Cannot extract logs. The driverID provided is null or empty.";
        }
        final Path logFilePath = Paths.get(filePath);
        List<String> logs = new ArrayList<>();
        try {
            if (!Files.exists(logFilePath)) {
                System.err.println("Error: Log file not found at " + filePath);
                return "ERROR: Log file not found.";
            }
            final String START_MARKER = getStrProp("LOG_MARKER_START", "Test case started");
            final String END_PASS_MARKER = getStrProp("LOG_MARKER_END_PASS", "Test case pass");
            final String END_FAIL_MARKER = getStrProp("LOG_MARKER_END_FAIL", "Test case fail");

            // Pre-calculate common regex patterns to avoid recalculating in the loop.
            // Pattern.quote() ensures special characters in the names are treated literally.
            final Pattern DRIVER_ID_PATTERN = Pattern.compile(".*" + Pattern.quote(driverID) + ".*");
            final Pattern TEST_CASE_NAME_PATTERN = Pattern.compile(".*" + Pattern.quote(testCaseName) + ".*");
            final Pattern START_MARKER_PATTERN = Pattern.compile(".*" + Pattern.quote(START_MARKER) + ".*");
            final Pattern END_PASS_MARKER_PATTERN = Pattern.compile(".*" + Pattern.quote(END_PASS_MARKER) + ".*");
            final Pattern END_FAIL_MARKER_PATTERN = Pattern.compile(".*" + Pattern.quote(END_FAIL_MARKER) + ".*");

            List<String> allLines = Files.readAllLines(logFilePath);
            boolean isCapturing = false;
            final int MAX_CAPTURE_LINES = getIntProp("LOG_MAX_CAPTURE_LINES", 500);
            int captureLineCount = 0;

            for (String line : allLines) {
                if (!isCapturing) {
                    boolean isDriverIDMatch = DRIVER_ID_PATTERN.matcher(line).matches();
                    boolean isTestCaseNameMatch = TEST_CASE_NAME_PATTERN.matcher(line).matches();
                    boolean isStartMarkerMatch = START_MARKER_PATTERN.matcher(line).matches();

                    boolean startCondition = isDriverIDMatch && isTestCaseNameMatch && isStartMarkerMatch;
                    if (startCondition) {
                        isCapturing = true;
                        logs.add(line);
                        continue;
                    }
                } else {
                    captureLineCount ++;
                    // Only capture lines that contain either the Driver ID or the Test Case Name
                    boolean currentLineMatchesContext = DRIVER_ID_PATTERN.matcher(line).matches() || TEST_CASE_NAME_PATTERN.matcher(line).matches();
                    if (currentLineMatchesContext) {
                        logs.add(line);
                        boolean isEndPassMarkerMatch = END_PASS_MARKER_PATTERN.matcher(line).matches();
                        boolean isEndFailMarkerMatch = END_FAIL_MARKER_PATTERN.matcher(line).matches();
                        boolean stopCondition = (isEndPassMarkerMatch || isEndFailMarkerMatch);
                        if (stopCondition) {
                            isCapturing = false;
                            break;
                        }
                    }
                    if (captureLineCount >= MAX_CAPTURE_LINES) {
                        System.err.println("Warning: Log extraction for test case " + testCaseName + " hit MAX_CAPTURE_LINES limit of " + MAX_CAPTURE_LINES + ".");
                        isCapturing = false;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("An unexpected error occurred while reading log file: " + filePath + ". Error: " + e.getMessage());
            return "ERROR: Failed to read log file due to IOException: " + e.getMessage();
        } catch (Exception e) {
            // Catch any unexpected runtime exceptions, e.g., issues with ConfigReader or null pointers
            System.err.println("An unexpected error occurred in LogExtractorUtil: " + e.getMessage());
            return "ERROR: An unexpected internal error occurred: " + e.getMessage();
        }
        return String.join("\n", logs);
    }
}