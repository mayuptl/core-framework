package core.base;

import com.aventstack.chaintest.plugins.ChainTestListener;
import io.github.bonigarcia.wdm.WebDriverManager;
import managers.CoreDriverManager;
import org.apache.logging.log4j.ThreadContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.testng.annotations.Listeners;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static core.config.CoreConfigReader.getStrProp;

/**
 * Base class for all test classes in the framework.
 *
 * <p>It handles the core functionality for **WebDriver initialization, management, and teardown**
 * in a thread-safe manner using {@code CoreDriverManager}. It supports various browsers,
 * WebDriverManager automatic setup, manual driver path specification, and parsing of
 * custom browser options (arguments, preferences, and capabilities).
 *
 * <p>It automatically registers {@code ChainTestListener} for test reporting integration.
 */
@Listeners(ChainTestListener.class)
public class CoreBaseTest {
    /**
     * Public default constructor for the base test class.
     * This allows derived test classes to inherit and instantiate the base class
     * for direct object creation when necessary.
     */
    public CoreBaseTest() {
        // Initialization logic, if any.
    }
    /**
     * The {@link WebDriver} instance for the current test thread.
     * This field is a convenience reference to the driver managed by {@code CoreDriverManager}
     * in the current thread's {@code ThreadLocal} storage.
     */
    public WebDriver driver;
    /**
     * Utility method to initialize the WebDriver, set implicit waits, and navigate to a starting URL.
     *
     * <p>NOTE: This method is typically annotated with {@code @BeforeMethod} or {@code @BeforeClass}
     * in a standard TestNG setup, but is shown here as a basic implementation demo using
     * hardcoded parameters.
     */
    // @BeforeClass
    public void lunchAppCore()
    {
        String CUSTOM_OPTIONS = "ARG:--force-device-scale-factor=0.8,ARG:--start-maximized,ARG:--incognito,ARG:--disable-infobars,ARG:--enable-logging=stderr,PREF:download.default_directory=/execution-output/test-downloads/,CAP:acceptInsecureCerts=true";
        String driverPath= "D:\\Work\\Automation\\app-utils\\app-utils\\notes\\msedgedriver.exe";
        //driver = initDriver("edge");
        //driver = initDriver("edge",driverPath);
        //driver = initDriverOptions("firefox",CUSTOM_OPTIONS);
        driver =  initDriver("edge",driverPath,CUSTOM_OPTIONS);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.get("https://mvnrepository.com/artifact/io.github.bonigarcia/webdrivermanager/6.3.2");
    }

    /**
     * Quits the current {@link WebDriver} instance and removes it from the thread-safe
     * {@code ThreadLocal} storage managed by {@link CoreDriverManager}.
     *
     * <p>It also clears the Log4j {@link ThreadContext} to ensure no context from the current
     * test thread leaks to the next test.
     */
    //  @AfterClass
    public void coreTearDown() {
        CoreDriverManager.quitDriver();
        ThreadContext.clearAll();
    }
    /**
     * Initializes the driver using the specified browser name, relying on
     * {@link WebDriverManager} to automatically download and configure the driver executable.
     *
     * @param BrowserName The name of the browser (e.g., "edge", "chrome", "firefox"). Can include "headless".
     * @return The initialized, thread-safe {@link WebDriver} instance.
     */
    public WebDriver initDriver(String BrowserName) {

        return initDriverCore(BrowserName, "", "");
    }
    /**
     * Initializes the driver using a manually specified path to the driver executable.
     *
     * @param BrowserName The name of the browser (e.g., "edge", "chrome", "firefox").
     * @param driverPath The manual path to the driver executable (e.g., "C:\\drivers\\geckodriver.exe").
     * @return The initialized, thread-safe {@link WebDriver} instance.
     */
    public WebDriver initDriver(String BrowserName, String driverPath) {
        return initDriverCore(BrowserName, driverPath, "");
    }
    /**
     * Initializes the driver using {@link WebDriverManager} and applies a custom options string.
     *
     * @param BrowserName The name of the browser (e.g., "edge", "chrome", "firefox").
     * @param customOptions The custom options string containing arguments (ARG:), preferences (PREF:), and capabilities (CAP:).
     * @return The initialized, thread-safe {@link WebDriver} instance.
     */
    public WebDriver initDriverOptions(String BrowserName, String customOptions) {
        return initDriverCore(BrowserName,"",customOptions );
    }
    /**
     * Initializes the driver using a manual path and applies a custom options string.
     * This provides the highest level of customization and explicit control over the driver setup.
     *
     * @param BrowserName The name of the browser (e.g., "edge", "chrome", "firefox").
     * @param driverPath The manual path to the driver executable.
     * @param customOptions The custom options string containing arguments (ARG:), preferences (PREF:), and capabilities (CAP:).
     * @return The initialized, thread-safe {@link WebDriver} instance.
     */
    public WebDriver initDriver(String BrowserName, String driverPath, String customOptions) {
        return initDriverCore(BrowserName, driverPath, customOptions);
    }
    /**
     * The core centralized method for configuring and initializing the {@link WebDriver}.
     *
     * <p>This method determines the browser type, handles the driver executable path (manual or WDM),
     * parses the comprehensive custom options string into browser-specific options
     * (arguments, preferences, capabilities), and stores the result in {@link CoreDriverManager}.
     *
     * @param BrowserName The name of the browser (e.g., "chrome", "edge headless").
     * @param driverPath The manual path to the driver executable (empty string if using WDM).
     * @param customOptions Comma-separated string of custom options ("ARG:...,PREF:...,CAP:...").
     * @return The initialized {@link WebDriver} instance.
     * @throws IllegalArgumentException If the specified {@code BrowserName} is not supported (e.g., "opera").
     */
    private WebDriver initDriverCore(String BrowserName, String driverPath, String customOptions) {
        WebDriver driver;
        // Map to hold preferences (for Chrome, Edge, Firefox)
        Map<String, Object> prefs = new HashMap<>();
        // Map to hold capabilities (used for options configuration)
        Map<String, Object> caps = new HashMap<>();
        // --- Options Parsing ---
        if (customOptions != null && !customOptions.isEmpty()) {
            String[] optionsArray = customOptions.split(",");
            for (String option : optionsArray) {
                String trimmedOption = option.trim();
                if (trimmedOption.startsWith("PREF:")) {
                    // Handle Browser Preferences (e.g., PREF:download.default_directory=/tmp)
                    try {
                        String prefString = trimmedOption.substring(5);
                        String[] parts = prefString.split("=", 2);
                        if (parts.length == 2) {
                            // Simple type parsing: if it looks like a boolean or number, try to cast it
                            Object value = parts[1];
                            if (value.toString().equalsIgnoreCase("true")) value = true;
                            else if (value.toString().equalsIgnoreCase("false")) value = false;
                            prefs.put(parts[0], value);
                        }
                    } catch (Exception e) {
                        System.err.println("WARN: Failed to parse PREF option: " + trimmedOption + ". Error: " + e.getMessage());
                    }
                } else if (trimmedOption.startsWith("CAP:")) {
                    // Handle General Capabilities (e.g., CAP:acceptInsecureCerts=true)
                    try {
                        String capString = trimmedOption.substring(4);
                        String[] parts = capString.split("=", 2);
                        if (parts.length == 2) {
                            // Simple string/boolean type conversion
                            Object value = parts[1].equalsIgnoreCase("true") ? true : (parts[1].equalsIgnoreCase("false") ? false : parts[1]);
                            caps.put(parts[0], value);
                        }
                    } catch (Exception e) {
                        System.err.println("WARN: Failed to parse CAP option: " + trimmedOption + ". Error: " + e.getMessage());
                    }
                }
                // Arguments are handled below (ARG: is optional, default is ARG)
            }
        }
        // --- Driver Initialization Logic ---
        if (BrowserName.contains("edge")) {
            EdgeOptions options = new EdgeOptions();
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.edge.driver", driverPath);
                // System.err.println("INFO: Using manual Edge driver path: " + driverPath);
            } else {
                WebDriverManager.edgedriver().setup();
            }
            // Edge uses 'ExperimentalOption' for preferences
            if (!prefs.isEmpty()) {
                options.setExperimentalOption("prefs", prefs);
            }
            if (BrowserName.contains("headless")) {
                options.addArguments("--headless=new");
            }
            // Apply arguments and capabilities
            applyOptions(options, customOptions, caps);
            driver = new EdgeDriver(options);

        } else if (BrowserName.contains("chrome")) {
            ChromeOptions options = new ChromeOptions();
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.chrome.driver", driverPath);
                // System.err.println("INFO: Using manual Chrome driver path: " + driverPath);
            } else {
                WebDriverManager.chromedriver().setup();
            }
            // Chrome uses 'prefs' for preferences
            if (!prefs.isEmpty()) {
                options.setExperimentalOption("prefs", prefs);
            }
            if (BrowserName.contains("headless")) {
                options.addArguments("--headless=new");
            }
            // Apply arguments and capabilities
            applyOptions(options, customOptions, caps);
            driver = new ChromeDriver(options);

        } else if (BrowserName.contains("firefox")) {
            FirefoxOptions options = new FirefoxOptions();
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.gecko.driver", driverPath);
                // System.err.println("INFO: Using manual Firefox driver path: " + driverPath);
            } else {
                WebDriverManager.firefoxdriver().setup();
            }
            // Firefox preferences are applied directly using addPreference
            prefs.forEach((key, value) -> {
                // Firefox options require String or Boolean/Integer types
                if (value instanceof Boolean) {
                    options.addPreference(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    options.addPreference(key, (Integer) value);
                } else {
                    options.addPreference(key, value.toString());
                }
            });
            if (BrowserName.contains("headless")) {
                options.addArguments("-headless"); // Firefox uses -headless
            }
            // Apply arguments and capabilities
            applyOptions(options, customOptions, caps);
            driver = new FirefoxDriver(options);

        } else if (BrowserName.contains("safari")) {
            // Safari driver is managed by the OS and WebDriverManager setup is redundant
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.safari.driver", driverPath);
                // System.err.println("INFO: Using manual Safari driver path: " + driverPath);
            }
            // Safari does not support traditional command-line arguments like Chrome/Edge
            SafariOptions options = new SafariOptions();
            // Apply capabilities (arguments are ignored for Safari)
            caps.forEach(options::setCapability);
            driver = new SafariDriver(options);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported browser specified: %s. Supported browsers are: edge, chrome, firefox, safari, edge headless, chrome headless, firefox headless",BrowserName));
        }
        // --- Final Setup (Thread-Safe) ---
        // Store the initialized driver in the thread-safe container (DriverManager)
        CoreDriverManager.setDriver(driver);
        // Set the driver ID in the Log4j ThreadContext for logs separation (Thread-safe logging)
        ThreadContext.put("driverId",String.valueOf(System.identityHashCode(CoreDriverManager.getDriver())));
        this.driver = driver; // Update the local field for immediate use in the current thread if needed
        return driver;
    }
    /**
     * Helper method to apply command-line arguments (ARG:) and general capabilities (CAP:)
     * to the browser options object.
     *
     * <p>This method dynamically applies settings based on the concrete browser options class
     * (e.g., {@code ChromeOptions}, {@code FirefoxOptions}), which implements
     * {@code MutableCapabilities}.</p>
     *
     * @param options The {@link MutableCapabilities} instance (e.g., ChromeOptions) to which settings are applied.
     * @param customOptions The full custom options string used to extract command-line arguments (ARG:...).
     * @param caps A map of capabilities parsed from the custom options string (CAP:...).
     */
    private void applyOptions(MutableCapabilities options, String customOptions, Map<String, Object> caps)
    {
        // 1. Apply Command-Line Arguments (ARG:)
        if (customOptions != null && !customOptions.isEmpty())
        {
            String[] optionsArray = customOptions.split(",");
            for (String option : optionsArray)
            {
                String trimmedOption = option.trim();
                // Only process items that are arguments (ARG: or no prefix and no value assignment)
                if (!trimmedOption.isEmpty() && !trimmedOption.startsWith("PREF:") && !trimmedOption.startsWith("CAP:"))
                {
                    String arg = trimmedOption.startsWith("ARG:") ? trimmedOption.substring(4) : trimmedOption;
                    // We check the options type to ensure we call the right method
                    if (options instanceof ChromeOptions)
                    {
                        ((ChromeOptions) options).addArguments(arg);
                    } else if (options instanceof EdgeOptions)
                    {
                        ((EdgeOptions) options).addArguments(arg);
                    } else if (options instanceof FirefoxOptions)
                    {
                        ((FirefoxOptions) options).addArguments(arg);
                    }
                }
            }
        }
        // 2. Apply Capabilities (CAP:)
        caps.forEach(options::setCapability);
    }
}