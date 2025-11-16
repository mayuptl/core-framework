package Demo;

import core.screenshot.CoreScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

public class POM {
    WebDriver driver;
    Logger log= LogManager.getLogger("POM");
    public POM(WebDriver driver)
    {
        this.driver= driver;
    }

    public void logCheck()
    {
        log.info("This is from pom - logCheck");
    }

    public void logCheckAnother()
    {
        log.info("This is from pom - logCheckAnother");
        CoreScreenshotUtil.stepss("This is from pom - logCheckAnother");
    }

}
