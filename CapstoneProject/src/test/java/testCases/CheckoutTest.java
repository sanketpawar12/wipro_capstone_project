// testCases/CheckoutTests.java
package testCases;

import org.testng.Assert;
import org.testng.annotations.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.CheckoutPage;
import pages.LoginPage;
import pages.ProductPage;
import utils.DriverFactory;

import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CheckoutTest {
    WebDriver driver;
    ProductPage productPage;
    CheckoutPage checkoutPage;
    LoginPage loginPage;

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.initDriver("chrome");

        // ensure we are on the application home page first
        driver.get("https://bstackdemo.com/");
        driver.manage().window().maximize();

        // wait for product list to be present before creating page objects
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        shortWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));

        // create page objects after navigation
        productPage = new ProductPage(driver);
        checkoutPage = new CheckoutPage(driver);
        loginPage = new LoginPage(driver);
    }

    /**
     * End-to-end up to shipping submit.
     * Ensures user is signed in first, then adds product, goes to checkout and submits shipping.
     */
    @Test
    public void checkoutShippingAndSubmitTest() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // ensure signed-in: open modal and pick demo user (your LoginPage handles dropdown)
        loginPage.openLoginForm();

        // choose username/password and click login
        loginPage.selectUsername("demouser");   // adjust if you prefer another account label
        loginPage.selectPassword("testingisfun99");
        loginPage.clickLogin();

        // wait for a post-login signal - title change or user menu - adjust selector if your app shows a user element
        try {
            wait.until(d -> d.getTitle().toLowerCase().contains("stackdemo") || d.findElements(By.cssSelector(".user-info, #user, .logout")).size() > 0);
        } catch (Exception e) {
            System.out.println("DEBUG: Post-login signal not detected within timeout. Current title: " + driver.getTitle());
        }

        // wait a brief moment to ensure page is stable
        Thread.sleep(500);

        // add first product to cart (ProductPage prints debug count if no products)
        productPage.addFirstProductToCart();

        // wait for cart items to appear (cart shelf item)
        try {
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                    By.cssSelector("div.float-cart__shelf-container .shelf-item"), 0));
        } catch (Exception e) {
            System.err.println("DEBUG: Cart did not update after adding product. Page title: " + driver.getTitle());
            System.err.println("DEBUG: Current URL: " + driver.getCurrentUrl());
            // fail early with helpful message
            Assert.fail("Cart did not update after adding product.");
        }

        // Optionally verify the cart contains at least one item
        Assert.assertTrue(
            driver.findElements(By.cssSelector("div.float-cart__shelf-container .shelf-item")).size() > 0,
            "Cart has no items after adding product."
        );

        // navigate to checkout page (your existing flow does this)
        driver.get("https://bstackdemo.com/checkout");

        // wait for checkout form to appear using your CheckoutPage helper
        checkoutPage.waitForForm();

        // fill shipping form with test data
        checkoutPage.fillShippingForm("Hari", "J", "123 Demo St", "CA", "90001");

        // submit shipping and wait for progress to payment/confirmation
        checkoutPage.submitShipping();

        boolean progressed = false;
        try {
            // Wait until either URL contains /confirmation OR presence of download or payment indicators
            progressed = wait.until(d ->
                    d.getCurrentUrl().contains("/confirmation")
                    || d.findElements(By.id("downloadpdf")).size() > 0
                    || d.findElements(By.cssSelector("div.payment-form, #payment, form.payment")).size() > 0
            );
        } catch (Exception ignored) {}

        // Final assert: we expect to have progressed to confirmation/payment
        Assert.assertTrue(progressed, "After submitting shipping we did not reach payment/confirmation. Current URL: " + driver.getCurrentUrl());
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
