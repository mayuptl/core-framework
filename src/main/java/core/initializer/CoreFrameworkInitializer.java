package core.initializer;

import core.config.CoreConfigReader;
import org.apache.logging.log4j.core.config.Configurator;
import java.io.IOException;

public class CoreFrameworkInitializer {
    private static boolean initialized = false;
    public static synchronized void init() throws IOException
    {
        if(initialized) return;
        CoreConfigReader.getStrProp("BROWSER");
        Configurator.initialize(null,"src/main/resources/log4j2.xml");
        initialized = true;
    }
}
