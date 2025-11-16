package Demo;

import core.base.CoreBaseTest;
import core.screenshot.CoreScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static core.config.CoreConfigReader.getStrProp;

public class TC03 extends CoreBaseTest {

    @BeforeClass
    public void setup()
    {
        Logger classLog= LogManager.getLogger("TC03");
        classLog.info("End to end start\n");
    }
    @AfterClass
    public void tear()
    {
        Logger classLog= LogManager.getLogger("TC03");
        classLog.info("End to end completed\n");
    }
    @Test(priority = 1)
    public void Glasgow_()
    {
        Logger log= LogManager.getLogger("Glasgow_");
        POM pom = new POM(driver);
        POMTwo pomtwo = new POMTwo(driver);
        log.info("Test case started");
        CoreScreenshotUtil.stepss("Glasgow_");
        pom.logCheck();
        pomtwo.logCheck();
        log.info("This test1 log to test driver id logic This test1 log to test driver id logic This test1 log to test driver id logic");
        pomtwo.logCheckAnother();
        pom.logCheckAnother();
     /*   Assert.fail();
        Assert.assertTrue(true);
        log.info("Test case pass");
        log.warn("after pass test");*/
    }
    @Test(priority = 2)
    public void London()
    {
        Logger log= LogManager.getLogger("London");
        log.info("Test case started");
        POM pom = new POM(driver);
        pom.logCheckAnother();
        CoreScreenshotUtil.stepss("London");
        log.info("This test2 log to test driver id logic");
        Assert.assertTrue(true);
        log.info("Test case pass\n");
    }
     @Test
    public void ttt()
    {
       String DEFAULT_REPORT_PATH = getStrProp("REPORT_OUTPUT_DIR","execution-output/test-reports/")+getStrProp("REPORT_NAME","ExtentReport.html");
       System.out.println(DEFAULT_REPORT_PATH);
    }
}
