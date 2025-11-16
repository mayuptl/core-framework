package listeners;

import core.initializer.CoreFrameworkInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import java.io.IOException;

public class CoreFrameworkExecutionListener implements IExecutionListener {

    @Override
    public void onExecutionStart()
    {
        try{
            CoreFrameworkInitializer.init();
            System.out.println("INFO: core-framework code initialization successful.");
        } catch (IOException e) {
           Logger log = LogManager.getLogger("FrameworksExecutionListener");
           log.error("FATAL: core-framework code initialization failed during onExecutionStart.");
        }
    }
    @Override
    public void onExecutionFinish()
    {
       /* ExtentReports extent = ExtentManager.getReportInstance();
        if(extent != null) {
            extent.flush();
        }*/
    }

}
