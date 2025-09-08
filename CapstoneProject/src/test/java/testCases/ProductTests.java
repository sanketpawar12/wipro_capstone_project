// testCases/ProductTests.java
package testCases;

import org.testng.Assert;
import org.testng.annotations.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;

import pages.ProductPage;
import pages.CartPage;
import utils.DriverFactory;

public class ProductTests {
    WebDriver driver;
    ProductPage productPage;
    CartPage cartPage;

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.initDriver("chrome");

        // NAVIGATE to the app under test — change to your real URL if different
        driver.get("https://bstackdemo.com/");
        driver.manage().window().maximize();

        // wait for product container to be present (gives page time to render products)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));
        } catch (Exception e) {
            System.err.println("DEBUG: products not loaded after 10s. Title: " + driver.getTitle());
            System.err.println("DEBUG: Current URL: " + driver.getCurrentUrl());
            System.err.println("DEBUG: Page source length: " + driver.getPageSource().length());
            throw e;
        }

        productPage = new ProductPage(driver);
        cartPage = new CartPage(driver);
    }

    @Test
    public void addFirstProductToCartTest() throws InterruptedException {
        productPage.addFirstProductToCart();

        // wait for cart items to appear
        new WebDriverWait(driver, Duration.ofSeconds(5))
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("div.float-cart__shelf-container .shelf-item"), 0));

        System.out.println("Cart items: " + cartPage.getCartItemNames());
        Assert.assertTrue(cartPage.isCartOpen(), "Cart did not open or no items present.");
    }

    @Test
    public void addNamedProductToCartTest() throws InterruptedException {
        String pname = "iPhone 12 Mini"; // updated product name
        boolean added = productPage.addProductToCartByName(pname);

        // wait for cart items to appear
        new WebDriverWait(driver, Duration.ofSeconds(5))
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("div.float-cart__shelf-container .shelf-item"), 0));

        System.out.println("Cart items: " + cartPage.getCartItemNames());
        Assert.assertTrue(added, "Product with name containing '" + pname + "' was not found/added.");
        Assert.assertTrue(
            cartPage.getCartItemNames().stream()
                    .anyMatch(n -> n.toLowerCase().contains(pname.toLowerCase())),
            "Cart does not contain the expected product."
        );
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
