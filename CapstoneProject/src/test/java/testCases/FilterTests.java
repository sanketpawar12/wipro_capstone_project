package testCases;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.HomePage;
import pages.SearchPage;
import utils.ConfigReader;

import java.time.Duration;

public class FilterTests {
    WebDriver driver;
    HomePage homePage;
    SearchPage searchPage;

    @BeforeClass
    public void setUp() {
        ConfigReader.loadConfig("testdata/config.properties");
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        driver.get(ConfigReader.getProperty("url"));
        homePage = new HomePage(driver);
        searchPage = new SearchPage(driver);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(homePage.getSearchBox()));
    }

    @DataProvider(name = "limitedFilterData")
    public Object[][] limitedFilterData() {
        return new Object[][] {
            {"Apple", "Lowest to highest"},
            {"Samsung", "Highest to lowest"}
        };
    }

    @Test(dataProvider = "limitedFilterData")
    public void testFilterAndSort(String vendor, String sortOrder) {
        driver.navigate().refresh();
        searchPage.applyVendorFilter(vendor);
        searchPage.selectSortOrder(sortOrder);
        Assert.assertTrue(searchPage.verifyVendorOnly(vendor), "Vendor check failed: " + vendor);
        Assert.assertTrue(searchPage.verifySortingByControl(sortOrder), "Sort check failed: " + sortOrder);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}



