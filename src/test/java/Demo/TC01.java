package Demo;

import core.base.CoreBaseTest;
import core.screenshot.CoreScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static core.wait.CoreWaitUtil.staticWait;

public class TC01 extends CoreBaseTest {
    @BeforeClass
    public void setup()
    {
       // DriverManager.getDriver();
        Logger classLog= LogManager.getLogger("TC01");
        classLog.info("End to end start\n");
    }
    @AfterClass
    public void tear()
    {
        Logger classLog= LogManager.getLogger("TC01");
        classLog.info("End to end completed\n");
    }
    @Test(priority = 1)
    public void LoginCheck_()
    {
        Logger log= LogManager.getLogger("LoginCheck_");
        POM pom = new POM(driver);
        POMTwo pomtwo = new POMTwo(driver);
        log.info("Test case started");
        System.err.println(driver.getCurrentUrl());
        staticWait(3);
        CoreScreenshotUtil.stepss("current url");
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
    public void LogOutCheck_()
    {
        Logger log= LogManager.getLogger("LogOutCheck_");
        log.info("Test case started");
       /* POM pom = new POM(driver);
        pom.logCheckAnother();
        ScreenshotUtil.stepss("LogOutCheck_");
        log.info("This test2 log to test driver id logic");
        Assert.assertTrue(true);*/
        log.info("Test case pass\n");
    }
}
