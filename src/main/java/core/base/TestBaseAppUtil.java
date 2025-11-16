package core.base;

import com.aventstack.chaintest.plugins.ChainTestListener;
import io.github.bonigarcia.wdm.WebDriverManager;
import managers.DriverManager;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static core.config.ConfigReader.getStrProp;

/**
 * Base utility class for initializing, managing, and tearing down WebDriver instances.
 * This class provides methods to launch the application using different configurations.
 */
@Listeners(ChainTestListener.class)
public class TestBaseAppUtil {
    /**
     * The WebDriver instance for the current test thread.
     * This field is often synchronized with the driver managed by {@code DriverManager}.
     */
    public WebDriver driver;
    /**
     * Initializes the WebDriver, sets implicit waits, and navigates to the starting URL.
     * <p>
     * NOTE: This method currently uses hardcoded initialization parameters for demonstration/local testing.
     * For production test suites, configuration should be loaded dynamically (e.g., from properties).
     */
    @BeforeClass
    public void lunchAppUtil()
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
     * Quits the current WebDriver instance and removes it from the ThreadLocal storage
     * via a single call to the DriverManager utility.
     */
  //  @AfterClass
    public void tearDownAppUtil() {
        DriverManager.quitDriver();
        ThreadContext.clearAll();
    }
    /**
     * Public method to initialize the driver with user define browser (using WebDriverManager).
     *
     * @param BrowserName The name of the browser. edge,chrome,firefox etc. (Can include "headless")
     * @return The initialized WebDriver thread safe instance.
     */
    public WebDriver initDriver(String BrowserName) {

        return initDriverCore(BrowserName, "", "");
    }
    /**
     * Public method to initialize the driver with a manual path.
     *
     * @param BrowserName The name of the browser. edge,chrome,firefox etc
     * @param driverPath The manual path to the driver executable.
     * @return The initialized WebDriver thread safe instance.
     */
    public WebDriver initDriver(String BrowserName, String driverPath) {
        return initDriverCore(BrowserName, driverPath, "");
    }
    /**
     * Public method to initialize the driver with user define browser and custom options.
     *
     * @param BrowserName The name of the browser. edge,chrome,firefox etc.
     * @param customOptions The custom options string ("ARG:...,ARG:...,PREF:...,PREF:...,CAP:...,CAP:...")
     * @return The initialized WebDriver thread safe instance.
     */
    public WebDriver initDriverOptions(String BrowserName, String customOptions) {
        return initDriverCore(BrowserName,"",customOptions );
    }
    /**
     * Public method to initialize the driver with a manual path and custom options.
     *
     * @param BrowserName The name of the browser. edge,chrome,firefox etc
     * @param driverPath The manual path to the driver executable.
     * @param customOptions The custom options string ("ARG:...,ARG:...,PREF:...,PREF:...,CAP:...,CAP:...")
     * @return The initialized WebDriver thread safe instance.
     */
    public WebDriver initDriver(String BrowserName, String driverPath, String customOptions) {
        return initDriverCore(BrowserName, driverPath, customOptions);
    }
    /**
     * Core method for driver initialization.
     * It handles WebDriverManager setup, manual driver path configuration,
     * and parsing of custom options for arguments, preferences, and capabilities.
     *
     * @param BrowserName The name of the browser (e.g., "chrome", "edge headless").
     * @param driverPath The manual path to the driver executable.
     * @param customOptions Comma-separated string of custom options ("ARG:...,ARG:...,PREF:...,PREF:...,CAP:...,CAP:...")
     * @return The initialized WebDriver instance.
     * @throws IllegalArgumentException If the specified browser name is not supported (e.g., "opera").
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
                System.err.println("INFO: Using manual Edge driver path: " + driverPath);
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
                System.err.println("INFO: Using manual Chrome driver path: " + driverPath);
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
                System.err.println("INFO: Using manual Firefox driver path: " + driverPath);
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
                System.err.println("INFO: Using manual Safari driver path: " + driverPath);
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
        DriverManager.setDriver(driver);
        // Set the driver ID in the Log4j ThreadContext for logs separation (Thread-safe logging)
        ThreadContext.put("driverId",String.valueOf(System.identityHashCode(DriverManager.getDriver())));
        this.driver = driver; // Update the local field for immediate use in the current thread if needed
        return driver;
    }
    /**
     * Helper method to apply command-line arguments (ARG:) and general capabilities (CAP:)
     * to the browser options object.
     *
     * <p>This method dynamically calls {@code addArguments} or {@code setCapability}
     * based on the concrete implementation of the {@code MutableCapabilities} instance
     * (e.g., {@code ChromeOptions}, {@code FirefoxOptions}).</p>
     *
     * @param options The MutableCapabilities instance (e.g., ChromeOptions) to which settings are applied.
     * @param customOptions The full custom options string used to extract command-line arguments.
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