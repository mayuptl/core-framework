package listeners;

import core.initializer.CoreFrameworkInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import java.io.IOException;

/**
 * TestNG {@link IExecutionListener} implementation responsible for managing the
 * lifecycle of the entire test execution (suite or run).
 * <p>
 * Its primary function is to initialize the framework's core configuration and
 * logging setup before any test is run.
 */
public class CoreFrameworkExecutionListener implements IExecutionListener {
    /**
     * Public constructor required by TestNG to instantiate this listener class
     * and hook into the test execution lifecycle events.
     */
    public CoreFrameworkExecutionListener() { }
    /**
     * Called by TestNG before the start of any test execution.
     * <p>
     * Triggers the framework initialization method (e.g., loading properties,
     * setting up log context) and logs the outcome.
     */
    @Override
    public void onExecutionStart()
    {
        try{
            CoreFrameworkInitializer.init();
            System.out.println("[INFO] Core-framework code initialization successful.");
        } catch (IOException e) {
            System.err.println("[FATAL] Core-framework code initialization failed during onExecutionStart.");
            Logger log = LogManager.getLogger("FrameworksExecutionListener");
            log.error("[FATAL] Core-framework code initialization failed during onExecutionStart.");
        }
    }
    /**
     * Called by TestNG after the completion of all test executions.
     * <p>
     * This method is typically used to perform final actions, such as flushing
     * reports (if not handled by other listeners) or cleaning up global resources.
     */
    @Override
    public void onExecutionFinish()
    {
       /* ExtentReports extent = ExtentManager.getReportInstance();
        if(extent != null) {
            extent.flush();
        }*/
    }
}