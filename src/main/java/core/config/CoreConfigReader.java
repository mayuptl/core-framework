package core.config;

import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Provides a central, static access point for all application configurations.
 *
 * <p>This class implements a multi-stage configuration loading mechanism:
 * <ol>
 * <li>Loads default properties from **inside** the JAR (low priority).</li>
 * <li>Loads overriding properties from an **external** consumer config file (high priority).</li>
 * <li>Loads and merges a secondary configuration set (e.g., "chaintest" properties).</li>
 * <li>Injects certain configuration values into Java System Properties for use by external libraries (like Log4j2).</li>
 * </ol>
 *
 * <p>The configuration is loaded only once during the class's static initialization block,
 * ensuring fast and consistent access across the application lifecycle.
 */
public class CoreConfigReader {
    /** Private constructor for a utility class; all methods are static. */
    private CoreConfigReader() { }
    /**
     * Static instance to hold the merged properties loaded from configuration files.
     * This cache is loaded only once during framework initialization to ensure fast, consistent access
     * to configuration values across all threads.
     */
    private static final Properties CACHED_PROPS = new Properties();
    /**
     * A list used to track details of properties that were overridden at runtime
     * (e.g., via Maven command line or environment variables) for logging and debugging purposes.
     * Each entry typically stores the key, old value, and new value.
     */
    private static final List<String[]> OVERRIDDEN_DETAILS = new ArrayList<>();
    /**
     * Static instance to hold the merged properties specifically for Counter-based keys.
     * This is used to maintain a separate, initialized cache for configuration elements
     * that involve numerical counting or sequencing.
     */
    private static final Properties CACHED_PROPS_CTR = new Properties();

    /**
     * Executes the configuration loading process. This static block is executed
     * once when the {@code CoreConfigReader} class is loaded by the JVM.
     *
     * <p>Any critical failure during configuration loading results in an error message
     * to System.err and a stack trace.
     */
    static {
        try {
            loadMergedProperties();
        } catch (Exception e) {
            System.err.println("FATAL ERROR: ConfigReader failed to initialize JAR's config.properties in static block!");
            System.err.println("This is likely due to a NullPointerException while reading a system property or an IOException while accessing a file.");
            e.printStackTrace();
            // Note: If you want the program to halt entirely upon a config load failure,
            // throw new RuntimeException("Failed to initialize core configuration.", e);
        }
    }
    /**
     * Loads the core and secondary configuration properties with the correct merging priority.
     *
     * <p>This method orchestrates the loading steps using the {@code CoreConfigLoaderUtils}:
     * <ol>
     * <li>Load JAR defaults into {@code CACHED_PROPS}.</li>
     * <li>Load consumer overrides into {@code CACHED_PROPS} and record overrides.</li>
     * <li>Inject core logging properties into System Properties for Log4j2.</li>
     * <li>Load secondary JAR defaults into {@code CACHED_PROPS_CTR}.</li>
     * <li>Load consumer overrides for secondary config into {@code CACHED_PROPS_CTR}.</li>
     * <li>Inject final secondary properties into System Properties.</li>
     * <li>Print override details if enabled.</li>
     * </ol>
     */
    private static void loadMergedProperties() {
        // Step 1 : Load Jars properties
        CoreConfigLoaderUtils.loadJarProp(CACHED_PROPS,"/core-config.properties");
        // Step 2 : Load consumer properties
        CoreConfigLoaderUtils.loadConsumerProp(CACHED_PROPS,OVERRIDDEN_DETAILS,"config.properties");
        // Step 3 : Inject Log4j2 values, get it from .prop file and inject to log4j2.xml file
        CoreConfigLoaderUtils.injectSystemPropToLog4j2Xml(CACHED_PROPS,"LOG_ROOT_LEVEL", "log4j2.rootLevel");
        CoreConfigLoaderUtils.injectSystemPropToLog4j2Xml(CACHED_PROPS,"LOG_CONSOLE_PATTERN", "log4j2.consolePattern");
        CoreConfigLoaderUtils.injectSystemPropToLog4j2Xml(CACHED_PROPS,"LOG_FILE_PATTERN", "log4j2.filePattern");
        CoreConfigLoaderUtils.injectSystemPropToLog4j2Xml(CACHED_PROPS,"LOG_OUTPUT_DIR", "log4j2.logDir");
        CoreConfigLoaderUtils.injectSystemPropToLog4j2Xml(CACHED_PROPS,"LOG_FILE_NAME", "log4j2.fileName");

        // Step 4 : Load Jars chiantest.propperties
        CoreConfigLoaderUtils.loadJarProp(CACHED_PROPS_CTR,"/chaintest.properties");
        // Step 5 : Load consumer properties
        CoreConfigLoaderUtils.loadConsumerCtr(CACHED_PROPS_CTR,OVERRIDDEN_DETAILS,"config.properties");
        // Step 6 : Inject consumers config.prop files chaintest key-values into jars system properties
        // This is the fix. It ensures the configuration is available immediately.
        CoreConfigLoaderUtils.injectFinalPropertiesAsSystemProps(CACHED_PROPS_CTR);

        // Step 7 : If consumer override Jar's .prop then it will print in log file
        if ("true".equalsIgnoreCase(getStrProp("LOG_SHOW_OVERRIDE"))) {
            CoreConfigLoaderUtils.printOverrideValue(OVERRIDDEN_DETAILS);
        }
    }
    // ****************************************************************************** //
    /**
     * Gets a String property value from the merged configuration.
     *
     * <p>Use this for **mandatory** properties. If the key is missing or the value is empty,
     * a {@code RuntimeException} is thrown to immediately halt execution.
     *
     * @param key The property key to look up.
     * @return The non-null, non-empty, trimmed String property value.
     * @throws RuntimeException if the key is missing or the value is empty.
     */
    public static String getStrProp(String key) {
        String value = CACHED_PROPS.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("❌ Missing or empty mandatory property for key: " + key);
        }
        return value.trim();
    }

    /**
     * Gets a String property value from the merged configuration, falling back to a default
     * if the key is missing or empty.
     *
     * <p>Use this for **optional** properties.
     *
     * @param key The property key to look up.
     * @param defaultValue The value to return if the key is missing or empty.
     * @return The trimmed property value, or the {@code defaultValue}.
     */
    public static String getStrProp(String key, String defaultValue) {
        String value = CACHED_PROPS.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Gets an integer property value from the merged configuration.
     *
     * <p>Use this for **mandatory** properties. Throws a {@code RuntimeException} if the key is missing
     * or the value cannot be parsed as an integer.
     *
     * @param key The property key to look up.
     * @return The property value as an {@code int}.
     * @throws RuntimeException if the key is missing or the value is not a valid integer.
     */
    public static int getIntProp(String key) {
        String value = getStrProp(key); // Will throw RuntimeException if key is missing/empty
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("❌ Invalid integer value for mandatory key: " + key + " -> " + value, e);
        }
    }

    /**
     * Gets an integer property value safely, using a default value if the key is missing,
     * empty, or the parsed value is invalid.
     *
     * <p>Use this for **optional** properties.
     *
     * @param key The property key to look up.
     * @param defaultValue The value to return if the key is missing or invalid.
     * @return The property value as an {@code int}, or the {@code defaultValue}.
     */
    public static int getIntProp(String key, int defaultValue) {
        String value = getStrProp(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // If the consumer provides an invalid value, log an error and return the default.
            System.err.println("ERROR: Invalid integer format for key: " + key + " -> " + value + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Gets a boolean property value from the merged configuration.
     *
     * <p>Use this for **mandatory** properties. Throws a {@code RuntimeException} if the key is missing.
     * Note: Boolean parsing follows {@code Boolean.parseBoolean()}, where any value other than
     * (case-insensitive) "true" returns {@code false}.
     *
     * @param key The property key to look up.
     * @return The property value as a {@code boolean}.
     * @throws RuntimeException if the key is missing or the value is empty.
     */
    public static boolean getBoolProp(String key) {
        String value = getStrProp(key);
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Gets a boolean property value safely, using a default value if the key is missing or empty.
     *
     * <p>Use this for **optional** properties.
     *
     * @param key The property key to look up.
     * @param defaultValue The value to return if the key is missing or empty.
     * @return The property value as a {@code boolean}, or the {@code defaultValue}.
     */
    public static boolean getBoolProp(String key, boolean defaultValue) {
        String value = getStrProp(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value.trim());
    }

    // ****************************************************************************** //
    /**
     * Helper method to load properties from a file located in the project's file system,
     * typically relative to the JVM's starting directory.
     *
     * @param filePath The relative or absolute path to the properties file (e.g., "config/test.properties").
     * @return The loaded {@link Properties} object.
     * @throws IOException if the file is not found or an I/O error occurs during loading.
     */
    private static Properties loadFromFileSystem(String filePath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            throw new IOException("❌ Config file not found: " + filePath +
                    "\n➡ Ensure the path is correct and the file exists.", e);
        } catch (IOException e) {
            throw new IOException("❌ Failed to load config file: " + filePath, e);
        }
        return props;
    }

    /**
     * Reads a String property from an **external file** specified by its path.
     *
     * <p>This method bypasses the main cached configuration and is used for non-core,
     * file-specific property access. Throws a {@code RuntimeException} if the file
     * cannot be read or the key is missing/empty.
     *
     * @param key The property key to look up in the external file.
     * @param filePath The path to the external properties file.
     * @return The non-null, non-empty, trimmed String property value.
     * @throws RuntimeException if the file cannot be read or the key is missing.
     */
    public static String getStrPropFromPath(String key, String filePath) {
        try {
            Properties prop = loadFromFileSystem(filePath);
            String value = prop.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                throw new RuntimeException("❌ Missing or empty property for key: " + key +
                        " in file: " + filePath);
            }
            return value.trim();
        } catch (IOException e) {
            throw new RuntimeException("❌ Unable to read property file: " + filePath, e);
        }
    }

    /**
     * Reads an integer property from an **external file** specified by its path.
     *
     * @param key The property key to look up in the external file.
     * @param filePath The path to the external properties file.
     * @return The property value as an {@code int}.
     * @throws RuntimeException if the file cannot be read, the key is missing, or the value is not a valid integer.
     */
    public static int getIntPropFromPath(String key, String filePath) {
        String value = getStrPropFromPath(key, filePath);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("❌ Invalid integer value for key: " + key +
                    " in file: " + filePath + " -> " + value);
        }
    }
    /**
     * Reads a boolean property from an **external file** specified by its path.
     *
     * @param key The property key to look up in the external file.
     * @param filePath The path to the external properties file.
     * @return The property value as a {@code boolean}.
     * @throws RuntimeException if the file cannot be read or the key is missing.
     */
    public static boolean getBoolPropFromPath(String key, String filePath) {
        String value = getStrPropFromPath(key, filePath);
        return Boolean.parseBoolean(value.trim());
    }
}