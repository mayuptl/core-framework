package core.initializer;

import core.config.CoreConfigReader;
import org.apache.logging.log4j.core.config.Configurator;
import java.io.IOException;

/**
 * Provides a central, thread-safe initializer for core framework components.
 *
 * <p>This class ensures that critical dependencies, such as the configuration properties
 * and the logging system, are initialized exactly once, regardless of how many
 * times {@code init()} is called across different threads.</p>
 */
public class CoreFrameworkInitializer {
    /** Private constructor for a utility class; all methods are static. */
    private CoreFrameworkInitializer() { }
    private static boolean initialized = false;
    /**
     * Initializes the core components of the framework in a thread-safe manner.
     *
     * <p>The initialization steps include:</p>
     * <ol>
     * <li>**Configuration Loading:** Triggering the static initialization block of
     * {@link CoreConfigReader} by accessing a property (e.g., "BROWSER"). This ensures
     * all core properties are loaded and merged before any other action.</li>
     * <li>**Logging Setup:** Configuring Log4j2 using the external {@code log4j2.xml}
     * resource file.</li>
     * </ol>
     *
     * @throws IOException If an I/O error occurs during configuration loading (e.g.,
     * if configuration files are specified but unreadable).
     */
    public static synchronized void init() throws IOException
    {
        if(initialized) return;
        // Accessing the configuration to trigger CoreConfigReader's static block.
        // This ensures properties are loaded and system properties are injected.
        CoreConfigReader.getStrProp("browser");
        // Initializes Log4j2 using the configuration file.
        Configurator.initialize(null,"src/main/resources/log4j2.xml");
        initialized = true;
    }
}