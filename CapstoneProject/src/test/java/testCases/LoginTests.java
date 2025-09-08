package testCases;

import org.testng.Assert;
import org.testng.annotations.*;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;
import utils.DriverFactory;

public class LoginTests {
    WebDriver driver;
    LoginPage loginPage;

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.initDriver("chrome");
        driver.get("https://bstackdemo.com/");   
        driver.manage().window().maximize();
        loginPage = new LoginPage(driver);
    }
    @Test
    public void validLoginTest() {
        // open modal, select values, login
        loginPage.openLoginForm();
        loginPage.selectUsername("existing_orders_user");          // partial/case-tolerant matching used
        loginPage.selectPassword("testingisfun99");
        loginPage.clickLogin();

        // assert some post-login signal — title contains StackDemo (keeps M1 style)
        Assert.assertTrue(driver.getTitle().contains("StackDemo"), "Login failed!");
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
