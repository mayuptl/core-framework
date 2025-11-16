package Demo;

import core.base.CoreBaseTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TC02 extends CoreBaseTest {

    @Test(priority = 1)
    public void EmpLoginCheck()
    {
        Logger log= LogManager.getLogger("EmpLoginCheck");
        log.info("Test case started");
        POM pom = new POM(driver);
        pom.logCheckAnother();
        Assert.assertTrue(true);
       /* log.info("Test case pass");*/
    }
    @Test(priority = 2)
    public void EmpLogoutCheck()
    {
        Logger log= LogManager.getLogger("EmpLogoutCheck");
        log.info("Test case started");
        POM pom = new POM(driver);
        pom.logCheck();
        Assert.assertTrue(true);
    }
}
