package managers;
import org.openqa.selenium.WebDriver;

/**
 * DriverManager uses ThreadLocal to store the WebDriver instance.
 * This ensures that each TestNG thread accesses its own, isolated WebDriver instance,
 * which is essential for safe and reliable parallel test execution.
 */
public class CoreDriverManager {

    // ThreadLocal variable to store the WebDriver instance for the current thread.
    // This provides the necessary isolation for parallel execution.
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    // Private constructor to prevent external instantiation of this utility class.
    private CoreDriverManager() {
        // Utility class: all methods are static.
    }
    /**
     * Retrieves the WebDriver instance associated with the current thread.
     * * @return The WebDriver instance for the current thread.
     * @throws IllegalStateException if no driver is currently set for the thread.
     */
    public static WebDriver getDriver()
    {
        WebDriver driver = threadLocalDriver.get();
        if (driver == null) {
            // This prevents a NullPointerException later in the test and gives a clear error message.
            throw new IllegalStateException("WebDriver is not initialized for the current thread. " +
                    "Ensure initDriver() has been called and completed successfully.");
        }
        return driver;
    }
    /**
     * Associates a WebDriver instance with the current thread.
     * This is called by TestBaseAppUtil after successfully creating the driver.
     * * @param driver The WebDriver instance to set.
     */
    public static void setDriver(WebDriver driver)
    {
        if (driver == null) {
            throw new IllegalArgumentException("Cannot set a null WebDriver instance in DriverManager.");
        }
        threadLocalDriver.set(driver);
    }
    /**
     * Safely quits the WebDriver instance associated with the current thread
     * and removes the reference from the ThreadLocal to prevent memory leaks.
     * This should be called in the teardown method (e.g., @AfterClass).
     */
    public static void quitDriver()
    {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error while quitting WebDriver for thread " + Thread.currentThread().getId() + ": " + e.getMessage());
            } finally {
                // CRITICAL: Remove the driver reference from ThreadLocal
                threadLocalDriver.remove();
            }
        }
    }
}
