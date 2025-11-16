package core.config;

import java.util.Map.Entry;
//import org.apache.logging.log4j.MarkerFactory;
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

public class CoreConfigReader {
    // Static instance to hold the merged properties, loaded only once
    private static final Properties CACHED_PROPS = new Properties();
    private static final List<String[]> OVERRIDDEN_DETAILS = new ArrayList<>();
    private static Logger LOGGER = null;
    private static final Properties CACHED_PROPS_CTR = new Properties();
    //  private static final Marker CONFIG_TABLE_DUMP_MARKER = MarkerFactory.getMarker("CONFIG_TABLE_DUMP");

    static {
        try {
            // These methods contain the critical logic that's currently failing.
            loadMergedProperties();
            loadMergedCtrProperties();
        } catch (Exception e) {
            System.err.println("FATAL ERROR: ConfigReader failed to initialize JAR's config.properties in static block!");
            System.err.println("This is likely due to a NullPointerException while reading a system property or an IOException while accessing a file.");
            e.printStackTrace();
            // Note: If you want the program to halt entirely upon a config load failure,
            // throw new RuntimeException("Failed to initialize core configuration.", e);
        }
    }
    /**
     * Loads the properties with the correct merging priority:
     * 1. Load the default properties from INSIDE the current JAR (lower priority).
     * 2. Overlay those defaults with the properties from the external consumer classpath (higher priority).
     * The result is stored in the static CACHED_PROPS object.
     */
    private static void loadMergedProperties() {
        InputStream defaultsStream = null;
        for (int i = 0; i < 2; i++) {
            try {
                defaultsStream = CoreConfigReader.class.getResourceAsStream("/core-config.properties");
                if (defaultsStream != null) {
                    CACHED_PROPS.load(defaultsStream);
                    //System.out.println("INFO: Loaded core-config.properties from inside JAR.\n");
                    break;
                } else {
                    System.err.println("WARNING: core-config.properties not found inside JAR (attempt " + (i + 1) + ")");
                }
            } catch (IOException e) {
                System.err.println("WARNING: Failed to load core-config.properties from inside JAR (attempt " + (i + 1) + ")");
            }
        }
        //Step 2 : Load consumer properties
        //List<String[]> overriddenDetails = new ArrayList<>();
        try (InputStream overrideStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties")) {
            if (overrideStream != null) {
                Properties overrideProps = new Properties();
                overrideProps.load(overrideStream);
                for (String key : overrideProps.stringPropertyNames()) {
                    String defaultValue = CACHED_PROPS.getProperty(key);
                    String overrideValue = overrideProps.getProperty(key);
                    if (defaultValue != null && !defaultValue.equals(overrideValue)) {
                        // Store Key, New Value, and Old Value as an array
                        OVERRIDDEN_DETAILS.add(new String[]{key, overrideValue, defaultValue});
                        /*System.out.printf("%n");
                        System.out.printf("INFO: User's config.properties value overrides JAR's value as belwo:%n");
                        System.out.printf("%s :(New value) %s | (Old value): %s %n", key, overrideValue, defaultValue);*/
                    }
                }
                CACHED_PROPS.putAll(overrideProps);
            }
        } catch (IOException e) {
            System.out.println("INFO: Using default values because no config.properties file not found or no keys match with JAR's core-config.properties file keys");
        }
        injectSystemProperty("LOG_ROOT_LEVEL", "log4j2.rootLevel");
        injectSystemProperty("LOG_CONSOLE_PATTERN", "log4j2.consolePattern");
        injectSystemProperty("LOG_FILE_PATTERN", "log4j2.filePattern");
        injectSystemProperty("LOG_OUTPUT_DIR", "log4j2.logDir");
        injectSystemProperty("LOG_FILE_NAME", "log4j2.fileName");
        // 2. PRINT OVERRIDES (Outside the loop, using formatted table)
        if ("true".equalsIgnoreCase(getStrProp("LOG_SHOW_OVERRIDE"))) {
            printOverrideValue(OVERRIDDEN_DETAILS);
        }
    }

    /**
     * Helper method to print the summary to the console and the detailed table to the log file.
     *
     * @param overriddenDetails List of [Key, NewValue, OldValue] arrays.
     */
    private static void printOverrideValue(List<String[]> overriddenDetails) {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger(CoreConfigReader.class);
        }
        if (overriddenDetails.isEmpty()) {
            //System.out.println("INFO: Override display is ENABLED, but no properties differed from JAR defaults.");
            // We stop here if the list is empty, having printed a useful message.
            return;
        }
        // Formatting constants for the log file table
        final int KEY_WIDTH = 30;   // Property Key column width
        final int VALUE_WIDTH = 40; // New Value and Old Value column widths
        // 1. Log the single summary line (goes to console and file)
        System.out.println("INFO: User's config.properties overrides JAR's defaults. See log file for full details.");
        // 2. Build the detailed table string only for the log file
        StringBuilder tableBuilder = new StringBuilder();
        // Format string: 30 chars | 40 chars | 40 chars
        final String ROW_FORMAT = "%-" + KEY_WIDTH + "s | %-" + VALUE_WIDTH + "s | %-" + VALUE_WIDTH + "s %n";
        final String SEPARATOR_LINE = "---------------------------------------------------------------------------------------------------------------------\n";
        final String HEADER_LINE = "=====================================================================================================================\n";
        // Append Header
        tableBuilder.append("\n").append(HEADER_LINE);
        tableBuilder.append("Configuration Override Details (Full Trace):\n");
        tableBuilder.append(HEADER_LINE);
        tableBuilder.append(String.format(ROW_FORMAT, "PROPERTY KEY", "NEW VALUE", "OLD VALUE (JAR Default)"));
        // Append Table Rows
        for (String[] details : overriddenDetails) {
            String key = details[0];
            String newValue = details[1];
            String oldValue = details[2];
            // Wrap text segments
            List<String> keySegments = wrapString(key, KEY_WIDTH);
            List<String> newValueSegments = wrapString(newValue, VALUE_WIDTH);
            List<String> oldValueSegments = wrapString(oldValue, VALUE_WIDTH);
            int maxLines = Math.max(keySegments.size(), Math.max(newValueSegments.size(), oldValueSegments.size()));
            tableBuilder.append(SEPARATOR_LINE);
            for (int i = 0; i < maxLines; i++) {
                // Extract segments, using empty strings for padding shorter values
                String currentKey = i < keySegments.size() ? keySegments.get(i) : "";
                String currentNewValue = i < newValueSegments.size() ? newValueSegments.get(i) : "";
                String currentOldValue = i < oldValueSegments.size() ? oldValueSegments.get(i) : "";

                tableBuilder.append(String.format(ROW_FORMAT, currentKey, currentNewValue, currentOldValue));
            }
        }
        tableBuilder.append(HEADER_LINE).append("\n");
        // 3. Log the detailed table string (this usually only goes to the file,
        // depending on the Log4j console appender configuration)
        LOGGER.trace(tableBuilder.toString());
        /*// 4. Log the detailed table string: Use TRACE level and the Marker
        logger.trace(CONFIG_TABLE_DUMP_MARKER, tableBuilder.toString());

        // 5. *** CRITICAL SYNCHRONIZATION FIX ***
        try {
            // Force Log4j2 to process all buffered events immediately and write to the file.
            LogManager.shutdown(0L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Log the synchronization failure.
            System.err.println("WARNING: Failed to synchronize Log4j2 during startup log flush: " + e.getMessage());
        }*/
    }

    /**
     * Helper method to wrap a string into a list of fixed-width segments.
     *
     * @param text  The string to wrap.
     * @param width The maximum width of each line segment.
     * @return A list of strings, each no longer than 'width'.
     */
    private static List<String> wrapString(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        int length = text.length();
        for (int i = 0; i < length; i += width) {
            // Extract substring segment
            lines.add(text.substring(i, Math.min(i + width, length)));
        }
        return lines;
    }

    /**
     * Helper method to map a configuration key's value to a system property key,
     * but only if the system property has not been set externally.
     */
    private static void injectSystemProperty(String configKey, String systemPropertyKey) {
        final String configValue = CACHED_PROPS.getProperty(configKey);
        // Only set the System Property if it hasn't been set yet
        if (System.getProperty(systemPropertyKey) == null) {
            if (configValue != null) {
                System.setProperty(systemPropertyKey, configValue);
            } else {
                // If a key needed for Log4j is missing, print a severe warning.
                System.err.println("WARNING: Configuration key '" + configKey + "' is missing from merged properties, cannot set Log4j system property.");
            }
        }
    }

    /**
     * Get a String property value. Throws RuntimeException if the key is missing.
     * Use this for MANDATORY properties.
     */
    public static String getStrProp(String key) {
        String value = CACHED_PROPS.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("❌ Missing or empty mandatory property for key: " + key);
        }
        return value.trim();
    }

    /**
     * Get a String property value, falling back to a default if the key is missing.
     * Use this for OPTIONAL properties.
     *
     * @param key          The property key to look up.
     * @param defaultValue The value to return if the key is missing or empty.
     * @return The property value or the default value.
     */
    public static String getStrProp(String key, String defaultValue) {
        String value = CACHED_PROPS.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Get an integer property value. Throws RuntimeException if the key is missing
     * or the parsed value is invalid.
     * Use this for MANDATORY properties.
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
     * Get an integer property value safely, using a default value if the key is missing
     * or the parsed value is invalid.
     * Use this for OPTIONAL properties.
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
     * Get a boolean property value. Throws RuntimeException if the key is missing.
     * Use this for MANDATORY properties.
     */
    public static boolean getBoolProp(String key) {
        String value = getStrProp(key);
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Get a boolean property value safely, using a default value if the key is missing.
     * Use this for OPTIONAL properties.
     */
    public static boolean getBoolProp(String key, boolean defaultValue) {
        String value = getStrProp(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Helper to read properties from a project-level directory (File System).
     * The path is resolved relative to the directory where the JVM process starts (usually project root).
     *
     * @param filePath The relative path from the project root (e.g., "test-config/core-config.properties").
     * @return The loaded Properties object.
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
     * Reads a String property safely from a given file path.
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
     * Reads an integer property from a given file path.
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
     * Reads a boolean property from a given file path.
     */
    public static boolean getBoolPropFromPath(String key, String filePath) {
        String value = getStrPropFromPath(key, filePath);
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Helper method to inject all properties from the final merged ChainTest config
     * into the Java System Properties. This guarantees the external library can read them.
     */
    private static void injectFinalPropertiesAsSystemProps() {
        if (CACHED_PROPS_CTR == null || CACHED_PROPS_CTR.isEmpty()) {
            return;
        }
        // Iterate over the final merged properties and set them as System Properties.
        for (Entry<Object, Object> entry : CACHED_PROPS_CTR.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            // Only set if the System Property hasn't been set externally (highest priority)
            if (System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        }
       // System.out.println("INFO: Injected final ChainTest properties into Java System Properties.");
    }

    public static void loadMergedCtrProperties() {
        // --- Step 1: Load JAR Defaults (Lowest Precedence) ---
        try (InputStream defaultsStreamCtr = CoreConfigReader.class.getResourceAsStream("/chaintest.properties")) {
            if (defaultsStreamCtr != null) {
                CACHED_PROPS_CTR.load(defaultsStreamCtr);
            } else {
                System.err.println("WARNING: chaintest.properties not found inside JAR. Using empty defaults.");
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load dchaintest.properties from JAR: " + e.getMessage());
        }
        // --- Step 2: Load Consumer Overrides (Highest Precedence) ---
        try (InputStream overrideStreamCtr = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties")) {
            if (overrideStreamCtr != null) {
                Properties overridePropsCtr = new Properties();
                overridePropsCtr.load(overrideStreamCtr);
                int overrideCount = 0;
                // CRITICAL LOGIC: Iterate and only override keys that exist in JAR defaults.
                Set<String> consumerKeys = overridePropsCtr.stringPropertyNames();
                for (String key : consumerKeys) {
                    if (CACHED_PROPS_CTR.containsKey(key)) {
                        CACHED_PROPS_CTR.setProperty(key, overridePropsCtr.getProperty(key));
                        overrideCount++;
                    }
                }
                // System.out.println("INFO: Override properties count : "overrideCount);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load consumer properties: " + e.getMessage());
        }
        // --- STEP 3: INJECT INTO SYSTEM PROPERTIES ---
        // This is the fix. It ensures the configuration is available immediately.
        injectFinalPropertiesAsSystemProps();
    }
}