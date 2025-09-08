package testCases;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;
import pages.HomePage;
import pages.SearchPage;
import utils.ConfigReader;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class SearchTests {
    WebDriver driver;
    HomePage homePage;
    SearchPage searchPage;

    @BeforeClass
    public void setUp() throws Exception {
        ConfigReader.loadConfig("testdata/config.properties");
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        driver.get(ConfigReader.getProperty("url"));
        homePage = new HomePage(driver);
        searchPage = new SearchPage(driver);
        Files.createDirectories(Paths.get("screenshots"));
    }

    @DataProvider(name = "searchData")
    public Object[][] getSearchData() throws InvalidFormatException {
        ConfigReader.setExcelFile("testdata/Search.xlsx", "Search");
        int rows = ConfigReader.getRowCount();
        Object[][] data = new Object[rows][1];
        for (int i = 0; i < rows; i++) data[i][0] = ConfigReader.getCellData(i + 1, 0);
        return data;
    }

    @Test(dataProvider = "searchData")
    public void testProductSearch(String product) {
        List<String> before = searchPage.getProductTitles().stream().map(WebElement::getText).sorted().collect(Collectors.toList());

        homePage.enterText(product);
        homePage.clickSearch();

        List<String> after = searchPage.getProductTitles().stream().map(WebElement::getText).sorted().collect(Collectors.toList());
        boolean isDisplayed = after.stream().anyMatch(t -> t.toLowerCase().contains(product.toLowerCase()));

        Assert.assertTrue(!after.equals(before) && isDisplayed,
                "Product list did not update or product '" + product + "' not found.");
    }

    @AfterMethod
    public void takeScreenshotOnFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            try {
                String product = result.getParameters()[0].toString().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path dest = Paths.get("screenshots", "ProductSearchFailed_" + product + ".png");
                org.openqa.selenium.io.FileHandler.copy(src, dest.toFile());
                System.out.println("Saved screenshot: " + dest);
            } catch (Exception ignored) {}
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}