package managers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;

import java.time.Duration;

/**
 * DriverFactory is responsible for creating and configuring the correct WebDriver instance
 * based on the specified browser type. It implements the Factory Method pattern.
 * <p>
 * This class uses WebDriverManager to handle the driver binaries automatically.
 */
public class DriverFactory {

    private static final String DEFAULT_BROWSER = "chrome";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final String BROWSER_PROPERTY = "browser";

    /**
     * Private constructor to prevent external instantiation of this utility class.
     */
    private DriverFactory() {
        // Utility class: all methods are static.
    }

    /**
     * Initializes and returns a new WebDriver instance based on the browser specified
     * in the system properties (or defaults to Chrome).
     *
     * @return A fully initialized WebDriver instance.
     * @throws IllegalArgumentException if the requested browser is not supported.
     */
    public static WebDriver createDriver() {
        String browserName = System.getProperty(BROWSER_PROPERTY, DEFAULT_BROWSER).toLowerCase();
        WebDriver driver;

        System.out.println("Initializing driver for browser: " + browserName);

        // This is where the core factory logic resides, selecting the correct driver.
        switch (browserName) {
            case "chrome":
                driver = createChromeDriver();
                break;
            case "firefox":
                driver = createFirefoxDriver();
                break;
            case "edge":
                driver = createEdgeDriver();
                break;
            case "safari":
                driver = createSafariDriver();
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser type specified: " + browserName +
                        ". Supported browsers are: chrome, firefox, edge, safari.");
        }

        configureDriver(driver);
        return driver;
    }

    /**
     * Configures common properties for the created WebDriver instance, such as
     * window maximization and implicit wait time.
     *
     * @param driver The WebDriver instance to configure.
     */
    private static void configureDriver(WebDriver driver) {
        // Maximize the browser window for consistent test execution
        driver.manage().window().maximize();

        // Set an implicit wait time for finding elements
        driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT);
    }

    // --- Browser-Specific Initialization Methods ---

    /**
     * Creates and configures a ChromeDriver instance.
     * @return A new ChromeDriver instance.
     */
    private static WebDriver createChromeDriver() {
        // Uses WebDriverManager to automatically download and setup the binary
        WebDriverManager.chromedriver().setup();

        // Recommended options for running tests consistently
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*"); // Crucial for modern Selenium/Chrome interaction

        // Headless mode can be added here if needed: options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    /**
     * Creates and configures a FirefoxDriver instance.
     * @return A new FirefoxDriver instance.
     */
    private static WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.addPreference("dom.webnotifications.enabled", false);
        return new FirefoxDriver(options);
    }

    /**
     * Creates and configures an EdgeDriver instance.
     * @return A new EdgeDriver instance.
     */
    private static WebDriver createEdgeDriver() {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        return new EdgeDriver(options);
    }

    /**
     * Creates and configures a SafariDriver instance.
     * Note: SafariDriver requires specific setup on macOS (e.g., enabling "Develop > Allow Remote Automation").
     * @return A new SafariDriver instance.
     */
    private static WebDriver createSafariDriver() {
        // Safari does not typically require WebDriverManager as the driver is built into macOS
        return new SafariDriver();
    }
}