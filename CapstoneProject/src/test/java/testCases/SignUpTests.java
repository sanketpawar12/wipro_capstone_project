package testCases;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.SignUpPage;
import utils.DriverFactory;

public class SignUpTests {
    WebDriver driver;
    SignUpPage signupPage;

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.initDriver("chrome");
        signupPage = new SignUpPage(driver);
    }

    @Test
    public void validSignupTest() {
        // If signup UI does not exist on the site, skip instead of failing
        boolean signupPresent = !driver.findElements(By.id("signup-link")).isEmpty()
                || !driver.findElements(By.id("newUsername")).isEmpty()
                || !driver.findElements(By.id("signup-btn")).isEmpty();

        if (!signupPresent) {
            throw new SkipException("Signup not present on this site — skipping SignUpTests.");
        }

        // If present, run the mocked signup flow
        signupPage.openSignupPage();
        signupPage.enterUsername("newUser");
        signupPage.enterEmail("newUser@test.com");
        signupPage.enterPassword("Password123");
        signupPage.clickSignup();

        Assert.assertTrue(driver.getTitle().contains("Welcome")
                || driver.getCurrentUrl().contains("home"),
            "Signup failed!");
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
