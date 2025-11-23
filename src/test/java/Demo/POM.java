package Demo;

import core.screenshot.CoreScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static core.highlight.CoreHighlightUtil.click;
import static core.highlight.CoreHighlightUtil.*;
import static core.wait.CoreWaitUtil.threadSleep;

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
        threadSleep(3000);
        isDisplayed(driver.findElement(By.cssSelector(".im-title")));
        threadSleep(8000);
        click(driver.findElement(By.cssSelector(".im-title")));
        CoreScreenshotUtil.stepss("This is from pom - logCheckAnother");
    }

}
