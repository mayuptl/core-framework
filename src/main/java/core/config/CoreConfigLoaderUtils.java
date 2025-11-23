package core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Utility class for loading, merging, and manipulating configuration properties.
 * Extracts low-level I/O and complex formatting logic from CoreConfigReader to keep
 * the main configuration access class clean and focused. This class provides the
 * mechanics for the two-stage configuration loading process: JAR defaults followed
 * by consumer overrides.
 */
final class CoreConfigLoaderUtils {

    /**
     * Prevents instantiation of this utility class.
     */
    private CoreConfigLoaderUtils() {}

    /**
     * Loads default properties from a resource file located inside the application JAR.
     * It attempts to load the resource twice to ensure robustness.
     *
     * @param CACHED_PROPS The {@link Properties} object to load the defaults into.
     * @param jarPropFilePath The path to the properties file inside the JAR (e.g., "/core-config.properties").
     */
    static void loadJarProp(Properties CACHED_PROPS, String jarPropFilePath) {
        InputStream defaultsStream = null;
        // Two attempts to load the resource
        for (int i = 0; i < 2; i++) {
            try {
                // Use CoreConfigReader.class to resolve the path correctly if it's in the same package
                defaultsStream = CoreConfigReader.class.getResourceAsStream(jarPropFilePath);
                if (defaultsStream != null) {
                    CACHED_PROPS.load(defaultsStream);
                    break;
                } else {
                    System.err.println("WARNING: " + jarPropFilePath + " is not found inside JAR (attempt " + (i + 1) + ")");
                }
            } catch (IOException e) {
                System.err.println("WARNING: Failed to load " + jarPropFilePath + " from inside JAR (attempt " + (i + 1) + ")");
            } finally {
                // The original code does not explicitly close the stream here, but a real application
                // should typically manage resource closure carefully.
            }
        }
    }

    /**
     * Loads consumer properties from the classpath (usually an external {@code config.properties} file)
     * and merges them into the cached properties, giving them high priority.
     *
     * <p>It also records any keys that override existing JAR default values.
     *
     * @param CACHED_PROPS The target {@link Properties} object containing JAR defaults to be merged.
     * @param OVERRIDDEN_DETAILS A list to record the details of properties that were overridden
     * (Key, NewValue, OldValue).
     * @param consumerPropFileName The name of the consumer properties file (e.g., "config.properties").
     */
    static void loadConsumerProp(Properties CACHED_PROPS, List<String[]> OVERRIDDEN_DETAILS, String consumerPropFileName) {
        try (InputStream overrideStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(consumerPropFileName)) {
            if (overrideStream != null) {
                Properties overrideProps = new Properties();
                overrideProps.load(overrideStream);
                // Record overrides
                for (String key : overrideProps.stringPropertyNames()) {
                    String defaultValue = CACHED_PROPS.getProperty(key);
                    String overrideValue = overrideProps.getProperty(key);
                    if (defaultValue != null && !defaultValue.equals(overrideValue)) {
                        OVERRIDDEN_DETAILS.add(new String[]{key, overrideValue, defaultValue});
                    }
                }
                // Merge all consumer properties (high priority)
                CACHED_PROPS.putAll(overrideProps);
            }
        } catch (IOException e) {
            System.out.println("INFO: Using default values because no config.properties file found or no keys matched with JAR's core-config.properties file keys");
        }
    }

    /**
     * Loads a specific consumer configuration file, primarily intended for "chaintest" properties,
     * and selectively overrides only those keys that already exist in the target JAR properties.
     *
     * <p>This ensures that the consumer can only override known keys within the specific properties set.
     *
     * @param CACHED_PROPS_CTR The target {@link Properties} object (e.g., for chaintest defaults) to be overridden.
     * @param OVERRIDDEN_DETAILS A list to record the details of properties that were overridden.
     * @param consumerPropFileName The name of the consumer properties file (e.g., "config.properties").
     */
    static void loadConsumerCtr(Properties CACHED_PROPS_CTR, List<String[]> OVERRIDDEN_DETAILS, String consumerPropFileName) {
        try (InputStream overrideStreamCtr = Thread.currentThread().getContextClassLoader().getResourceAsStream(consumerPropFileName)) {
            if (overrideStreamCtr != null) {
                Properties overridePropsCtr = new Properties();
                overridePropsCtr.load(overrideStreamCtr);
                // Only override keys that exist in JAR defaults (CACHED_PROPS_CTR)
                for (String key : overridePropsCtr.stringPropertyNames()) {
                    String defaultValue = CACHED_PROPS_CTR.getProperty(key);
                    String overrideValue = overridePropsCtr.getProperty(key);

                    if (defaultValue != null && !defaultValue.equals(overrideValue)) {
                        // This records an override for the 'core' config, which might be confusing
                        // if the chaintest keys are different. Assuming the key recording is correct
                        // as per original logic.
                        OVERRIDDEN_DETAILS.add(new String[]{key, overrideValue, defaultValue});
                    }
                    if (CACHED_PROPS_CTR.containsKey(key)) {
                        CACHED_PROPS_CTR.setProperty(key, overridePropsCtr.getProperty(key));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load consumer chaintest-related properties: " + e.getMessage());
        }
    }

    /**
     * Injects all properties from the final merged secondary configuration (e.g., ChainTest)
     * into the Java System Properties.
     *
     * <p>This makes the configuration globally available to libraries that rely on
     * {@code System.getProperty()}. It respects existing System Properties, ensuring
     * external command-line arguments (highest priority) are not overwritten.
     *
     * @param CACHED_PROPS_CTR The final merged {@link Properties} object to inject.
     */
    static void injectFinalPropertiesAsSystemProps(Properties CACHED_PROPS_CTR) {
        if (CACHED_PROPS_CTR == null || CACHED_PROPS_CTR.isEmpty()) {
            return;
        }
        for (Entry<Object, Object> entry : CACHED_PROPS_CTR.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            // Only set if the System Property hasn't been set externally (highest priority)
            if (System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        }
    }

    /**
     * Maps a value from the core configuration properties to a specific Java System Property,
     * typically used to configure Log4j2 parameters via the system properties mechanism.
     *
     * <p>The system property is only set if it hasn't already been defined externally.
     *
     * @param CACHED_PROPS The core {@link Properties} containing the configuration value.
     * @param configKey The key in the {@code CACHED_PROPS} to read the value from.
     * @param systemPropertyKey The name of the System Property to set (e.g., "log4j2.rootLevel").
     */
    static void injectSystemPropToLog4j2Xml(Properties CACHED_PROPS, String configKey, String systemPropertyKey) {
        final String configValue = CACHED_PROPS.getProperty(configKey);
        if (System.getProperty(systemPropertyKey) == null) {
            if (configValue != null) {
                System.setProperty(systemPropertyKey, configValue);
            } else {
                System.err.println("WARNING: Configuration key '" + configKey + "' is missing from merged properties, cannot set Log4j system property.");
            }
        }
    }
    /**
     * Prints a summary of overridden properties to the console and outputs a detailed
     *, formatted table of all overridden values to the log file at the TRACE level.
     *
     * @param overriddenDetails List of arrays, where each array contains [Key, NewValue, OldValue]
     * for a property that was overridden by the consumer config.
     */
    static void printOverrideValue(List<String[]> overriddenDetails) {
        // Since LOGGER is static in CoreConfigReader, it must be passed or accessed via a getter,
        // or re-initialized here. Re-initializing is safer for a utility class.
        final Logger LOGGER = LogManager.getLogger(CoreConfigReader.class);

        if (overriddenDetails.isEmpty()) {
            return;
        }
        final int KEY_WIDTH = 30;
        final int VALUE_WIDTH = 40;

        System.out.println("INFO: User's config.properties overrides JAR's defaults. See log file for full details.");

        StringBuilder tableBuilder = new StringBuilder();
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

            List<String> keySegments = wrapString(key, KEY_WIDTH);
            List<String> newValueSegments = wrapString(newValue, VALUE_WIDTH);
            List<String> oldValueSegments = wrapString(oldValue, VALUE_WIDTH);
            int maxLines = Math.max(keySegments.size(), Math.max(newValueSegments.size(), oldValueSegments.size()));

            tableBuilder.append(SEPARATOR_LINE);
            for (int i = 0; i < maxLines; i++) {
                String currentKey = i < keySegments.size() ? keySegments.get(i) : "";
                String currentNewValue = i < newValueSegments.size() ? newValueSegments.get(i) : "";
                String currentOldValue = i < oldValueSegments.size() ? oldValueSegments.get(i) : "";
                tableBuilder.append(String.format(ROW_FORMAT, currentKey, currentNewValue, currentOldValue));
            }
        }
        tableBuilder.append(HEADER_LINE).append("\n");

        LOGGER.trace(tableBuilder.toString());
    }

    /**
     * Helper method to wrap a string into a list of fixed-width segments.
     * This is typically used to format long property keys or values into
     * multi-line table cells for better log readability.
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
            lines.add(text.substring(i, Math.min(i + width, length)));
        }
        return lines;
    }
}