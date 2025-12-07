package core.video;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static core.config.CoreConfigReader.getStrProp;
/**
 * Utility class to locate the video recording file for a given test case and format
 * the file path as an HTML anchor tag for inclusion in test reports.
 * The default video file extension is set to ".avi".
 */
public class CoreVideoPathUtil {
    /** Private constructor for a utility class; all methods are static. */
    private CoreVideoPathUtil() { }
    /**
     * The default folder path where video recordings are expected to be stored.
     * Safely defaults to {@code execution-output/test-recordings/} if the configuration key is missing.
     */
    private static final String DEFAULT_VIDEO_FOLDER = getStrProp("video.output.dir", "execution-output/test-recordings/");
    // /** The default file extension used for video recordings (currently {@value #DEFAULT_VIDEO_EXTENSION}). */
    private static final String DEFAULT_VIDEO_EXTENSION = ".avi";
    /**
     * Extracts the file path for the video recording of a test case using the default video folder
     * and the default extension ({@value #DEFAULT_VIDEO_EXTENSION}).
     *
     * @param testCaseName The name of the test case (e.g., "loginTest").
     * @return A {@code String} containing the HTML anchor tag link to the video file, or an error message string.
     */
    public static String toGetVideoFilePath(String testCaseName)
    {
        // Calls the primary implementation with the default extension
        return toGetVideoFilePath(testCaseName,DEFAULT_VIDEO_FOLDER);
    }

    /**
     * Extracts the file path for the video recording of a test case using a custom video folder.
     *
     * @param testCaseName The name of the test case (e.g., "loginTest").
     * @param userPath     The custom directory path to search for the video file.
     * @return A {@code String} containing the HTML anchor tag link to the video file, or an error message string.
     */
    public static String toGetVideoFilePath(String testCaseName, String userPath) {
        // Core logic will handle path checks and return an error if userPath is invalid or file not found.
        return toGetVideoFilePathCoreLogic(testCaseName, userPath);
    }
    /**
     * Core logic to search for the video file and format its path as an HTML hyperlink.
     * The expected filename is constructed as {@code testCaseName + DEFAULT_VIDEO_EXTENSION}.
     *
     * @param testCaseName The name of the test case.
     * @param userPath The directory path to search in.
     * @return The HTML anchor tag linking to the video file, or a descriptive error message.
     */
    private static String toGetVideoFilePathCoreLogic(String testCaseName, String userPath)
    {
        final String fullFileName = testCaseName + DEFAULT_VIDEO_EXTENSION;
        File directory = new File(userPath);
        try {
            // 1. Check if the directory exists and is valid
            if (!directory.exists() || !directory.isDirectory())
            {
                String error = "VIDEO ERROR: Recording directory not found or is not accessible at: " + userPath;
                System.err.println(error);
                return error;
            }
            // 2. List files, safely defaulting to an empty array if listFiles() returns null (e.g., permission denied)
            File[] files = Objects.requireNonNullElse(directory.listFiles(), new File[0]);
            for (File file : files) {
                // 3. Check for the file name (case-insensitive for robustness)
                if (file.isFile() && file.getName().equalsIgnoreCase(fullFileName))
                {
                    // Use Path to get the absolute URI for proper URL construction
                    Path filepath = Paths.get(file.getAbsolutePath());
                    // file.toUri().toString() correctly handles protocol and slashes for all OS
                    String formattedPath = filepath.toUri().toString();
                    // String linkText = "Execution Video"; //Execution Video
                    // Return the HTML anchor tag
                    return "<a href=\"" + formattedPath + "\" target=\"_blank\">" + testCaseName + "</a>";
                }
            }
            // 4. If the loop finishes without finding the file
            String fileNotFoundError = "VIDEO NOT FOUND: File '" + fullFileName + "' not found in " + userPath;
            System.err.println(fileNotFoundError);
            return fileNotFoundError;
        } catch (Exception e) {
            // Catching any unexpected internal runtime exceptions (e.g., Path errors, severe file access issues)
            String unexpectedError = "VIDEO ERROR: An unexpected error occurred while processing video files: " + e.getMessage();
            System.err.println(unexpectedError);
            return unexpectedError;
        }
    }
}