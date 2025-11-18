package managers;

import core.video.CoreTestRecorderUtil;
import java.awt.*;
import java.io.IOException;

import static core.config.CoreConfigReader.getStrProp;
/**
 * Manages the TestRecorder instance to provide thread-safe video recording
 * for test execution.
 * It uses ThreadLocal to ensure that parallel tests each have their own
 * dedicated recorder instance, preventing concurrency issues.
 */
public class CoreRecorderManager {
    /** Private constructor for a utility class; all methods are static. */
    private CoreRecorderManager() { }
    /** The ThreadLocal variable holding the unique CoreTestRecorderUtil instance for the current thread. */
    private static final ThreadLocal<CoreTestRecorderUtil> recorderThread = new ThreadLocal<>();
    /** The default output directory path for saving video recordings, retrieved from configuration. */
    private static final String DEFAULT_VIDEO_FOLDER = getStrProp("VIDEO_OUTPUT_DIR");

    /**
     * Initializes the thread-local recorder with a specific video name and a custom path.
     * The recorder is initialized only once per thread.
     *
     * @param recordedVideoName The file name (without extension) for the video recording.
     * @param userPath The directory path where the video should be saved.
     * @throws IOException If an I/O error occurs during setup.
     * @throws AWTException If the platform does not allow low-level input control.
     */
    public static synchronized void initializeRecorder(String recordedVideoName, String userPath) throws IOException, AWTException {
        // Ensures only one instance creation happens safely
        if (recorderThread.get() == null) {
            CoreTestRecorderUtil recorder = CoreTestRecorderUtil.createConfiguredRecorder(recordedVideoName, userPath);
            recorderThread.set(recorder);
        }
    }
    /**
     * Initializes the thread-local recorder using the configured default path:
     * `${user.dir}/execution-output/test-recordings/`.
     *
     * @param recordedVideoName The file name (without extension) for the video recording.
     * @throws IOException If an I/O error occurs during setup.
     * @throws AWTException If the platform does not allow low-level input control.
     */
    public static synchronized void initializeRecorder(String recordedVideoName) throws IOException, AWTException {
        initializeRecorder(recordedVideoName, DEFAULT_VIDEO_FOLDER);
    }
    /**
     * Retrieves the **thread-local** TestRecorder instance for the current thread.
     *
     * @return The TestRecorder instance.
     * @throws IllegalStateException if {@code initializeRecorder()} has not been called for this thread.
     */
    public static CoreTestRecorderUtil getRecorder() {
        CoreTestRecorderUtil recorder = recorderThread.get();
        if (recorder == null) {
            throw new IllegalStateException("Recorder not initialized for this thread. Call initializeRecorder() first.");
        }
        return recorder;
    }
    /**
     * Removes the TestRecorder instance from the current thread's storage.
     * This method is **crucial** for cleaning up the thread state after the test finishes
     * to prevent memory leaks and ensure thread reusability.
     */
    public static void removeInstance() {
        recorderThread.remove();
    }
}