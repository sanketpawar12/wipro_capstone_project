package testCases;

import org.testng.Assert;
import org.testng.annotations.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import pages.LoginPage;
import pages.ProductPage;
import pages.CartPage;
import pages.CheckoutPage;
import utils.DriverFactory;

import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ConfirmationFlowTest {
    WebDriver driver;
    WebDriverWait wait;
    LoginPage loginPage;
    ProductPage productPage;
    CartPage cartPage;
    CheckoutPage checkoutPage;
    Path downloadDir;

    @BeforeMethod
    public void setup() {
        // set download directory for the test (created if missing)
        String downloads = System.getProperty("user.dir") + "/target/test-downloads";
        downloadDir = Path.of(downloads);

        // use the download-enabled driver
        driver = DriverFactory.initDriverWithDownload(downloadDir.toAbsolutePath().toString());
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(12));

        // start at home
        driver.get("https://bstackdemo.com/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));

        loginPage = new LoginPage(driver);
        productPage = new ProductPage(driver);
        cartPage = new CartPage(driver);
        checkoutPage = new CheckoutPage(driver);
    }

    @Test
    public void fullCheckoutFlow_login_add_checkout_confirm_download_continue_checkNavs_logout() throws Exception {
        // 1) Login
        loginPage.openLoginForm();
        loginPage.selectUsername("demouser");
        loginPage.selectPassword("testingisfun99");
        loginPage.clickLogin();

        wait.until(d -> d.findElements(By.xpath("//*[contains(text(),'demouser') or contains(.,'Logout') or contains(.,'logout')]")).size() > 0);

        // 2) Add a product
        String productName = "iPhone 12 Mini";
        boolean added = productPage.addProductToCartByName(productName);
        Assert.assertTrue(added, "Failed to add product: " + productName);

        // 3) Ensure cart open
        if (driver.findElements(By.cssSelector("div.float-cart")).isEmpty()) {
            String[] toggles = new String[] { "div.float-cart__header", "div.float-cart__toggle, .cart-toggle, .bag, button.float-cart__open", "header .bag" };
            for (String t : toggles) {
                if (driver.findElements(By.cssSelector(t)).size() > 0) {
                    driver.findElement(By.cssSelector(t)).click();
                    break;
                }
            }
        }

        wait.until(d -> d.findElements(By.cssSelector("div.float-cart .shelf-item")).size() > 0);
        Assert.assertTrue(cartPage.isCartOpen(), "Cart did not open or no items present after add.");

        // 4) Proceed to checkout
        List<WebElement> checkoutButtons = driver.findElements(By.cssSelector("div.buy-btn, button.checkout, a.checkout, .checkout-button"));
        boolean clicked = false;
        for (WebElement b : checkoutButtons) {
            try {
                if (b.isDisplayed() && b.isEnabled()) { b.click(); clicked = true; break; }
            } catch (Exception ignored) {}
        }
        if (!clicked) driver.get("https://bstackdemo.com/checkout");

        // 5) Fill shipping
        checkoutPage.waitForForm();
        checkoutPage.fillShippingForm("Test", "User", "123 Demo St", "CA", "90001");

        // 6) Submit shipping
        checkoutPage.submitShipping();

        // 7) Wait for confirmation
        boolean reachedConfirmation = wait.until(d ->
            d.getCurrentUrl().contains("/confirmation")
            || d.findElements(By.xpath("//*[contains(text(),'Your Order has been successfully placed') or contains(.,'Your order number')]")).size() > 0
        );
        Assert.assertTrue(reachedConfirmation, "Did not reach confirmation page. URL=" + driver.getCurrentUrl());

        // 8) Download PDF
        By downloadPdf = By.id("downloadpdf");
        WebElement dl = wait.until(ExpectedConditions.elementToBeClickable(downloadPdf));

        // clean dir
        if (!Files.exists(downloadDir)) Files.createDirectories(downloadDir);
        try (Stream<Path> s = Files.list(downloadDir)) {
            s.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        }

        dl.click();
        System.out.println("DEBUG: clicked download link");

        // wait for pdf
        Path found = null;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            try (Stream<Path> s = Files.list(downloadDir)) {
                Optional<Path> maybe = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf")).findFirst();
                if (maybe.isPresent()) {
                    found = maybe.get();
                    long size1 = Files.size(found);
                    Thread.sleep(300);
                    long size2 = Files.size(found);
                    if (size2 >= size1 && size2 > 0) break;
                }
            } catch (NoSuchFileException nsf) {
                // not yet created
            }
            Thread.sleep(500);
        }
        Assert.assertNotNull(found, "Downloaded PDF file not found in: " + downloadDir);
        System.out.println("DEBUG: Downloaded file: " + found.toAbsolutePath());

        // 9) Click Continue Shopping (robust)
        WebElement continueBtn = null;
        By[] continueLocators = new By[] {
            By.xpath("//button[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'CONTINUE SHOPPING')]"),
            By.xpath("//button[contains(normalize-space(.),'Continue Shopping')]"),
            By.cssSelector("button.button--tertiary, button.optimizedCheckout-buttonSecondary, .continueButtonContainer button"),
            By.cssSelector("a.continue, .continue, .btn-continue")
        };
        for (By loc : continueLocators) {
            try {
                if (driver.findElements(loc).size() > 0) { continueBtn = driver.findElement(loc); break; }
            } catch (Exception ignored) {}
        }
        Assert.assertNotNull(continueBtn, "Continue Shopping button not found.");
        try { wait.until(ExpectedConditions.elementToBeClickable(continueBtn)).click(); }
        catch (Exception e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn); }

        // 10) Wait for homepage
        wait.until(d -> !d.getCurrentUrl().contains("/confirmation") && d.findElements(By.cssSelector("div.shelf-item")).size() > 0);
        System.out.println("DEBUG: Back to homepage.");

        // === NEW: three navigation checks before logout ===

        // A) Orders: click nav item with id="orders" and assert orders page/heading
        try {
            By ordersNav = By.id("orders");
            wait.until(ExpectedConditions.elementToBeClickable(ordersNav)).click();
            // verify we navigated to /orders or an Orders heading exists
            wait.until(d -> d.getCurrentUrl().contains("/orders") || d.findElements(By.xpath("//*[contains(normalize-space(.),'Orders') and (self::h1 or self::h2 or self::strong or self::div)]")).size() > 0 );
            System.out.println("DEBUG: Orders page check passed. URL=" + driver.getCurrentUrl());
        } catch (Exception e) {
            Assert.fail("Orders navigation/check failed: " + e.getMessage());
        }

        // B) Favourites: click nav item with id="favourites" and verify
        try {
            By favNav = By.id("favourites");
            wait.until(ExpectedConditions.elementToBeClickable(favNav)).click();
            // wait for either URL contains /favourites or presence of a favourites area
            wait.until(d -> d.getCurrentUrl().contains("/favourites") || d.findElements(By.xpath("//*[contains(normalize-space(.),'Favourites') or contains(normalize-space(.),'Favorites')]")).size() > 0 );
            System.out.println("DEBUG: Favourites page check passed. URL=" + driver.getCurrentUrl());
        } catch (Exception e) {
            Assert.fail("Favourites navigation/check failed: " + e.getMessage());
        }

        // C) Offers: click nav item id="offers" and verify
        try {
            By offersNav = By.id("offers");
            wait.until(ExpectedConditions.elementToBeClickable(offersNav)).click();
            wait.until(d -> d.getCurrentUrl().contains("/offers") || d.findElements(By.xpath("//*[contains(normalize-space(.),'Offers') or contains(normalize-space(.),'Offer')]")).size() > 0 );
            System.out.println("DEBUG: Offers page check passed. URL=" + driver.getCurrentUrl());
        } catch (Exception e) {
            Assert.fail("Offers navigation/check failed: " + e.getMessage());
        }

        // wait a short moment so UI settles
        Thread.sleep(400);

        // === Continue with logout ===

        // 11) Logout
        WebElement logoutEl = null;
        By[] logoutSelectors = new By[] {
            By.xpath("//*[contains(normalize-space(.),'Logout') or contains(normalize-space(.),'logout')]"),
            By.cssSelector("nav .logout-link, .logout-link, a.logout, a[href*='logout']")
        };
        for (By sel : logoutSelectors) {
            try {
                List<WebElement> els = driver.findElements(sel);
                if (!els.isEmpty()) {
                    for (WebElement e : els) { if (e.isDisplayed()) { logoutEl = e; break; } }
                    if (logoutEl != null) break;
                }
            } catch (Exception ignored) {}
        }
        if (logoutEl == null) {
            By[] userMenuLocators = new By[] { By.cssSelector(".username, .user-menu, nav .user, .UserNav_root") };
            for (By um : userMenuLocators) {
                try {
                    if (driver.findElements(um).size() > 0) {
                        WebElement menu = driver.findElement(um);
                        try { menu.click(); } catch (Exception e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menu); }
                        Thread.sleep(300);
                        for (By sel : logoutSelectors) {
                            if (driver.findElements(sel).size() > 0) { logoutEl = driver.findElement(sel); break; }
                        }
                    }
                } catch (Exception ignored) {}
                if (logoutEl != null) break;
            }
        }

        Assert.assertNotNull(logoutEl, "Logout not found.");
        try { wait.until(ExpectedConditions.elementToBeClickable(logoutEl)).click(); }
        catch (Exception e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutEl); }

        // 12) Verify logout
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("signin")),
            ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[contains(text(),'demouser') or contains(text(),'DemoUser') or contains(text(),'demo user')]"))
        ));
        boolean usernameStillPresent = driver.findElements(By.xpath("//*[contains(text(),'demouser') or contains(text(),'DemoUser') or contains(text(),'demo user')]")).size() > 0;
        Assert.assertFalse(usernameStillPresent, "User still appears logged in.");

        System.out.println("DEBUG: End-to-end flow (with PDF, Continue Shopping, Orders/Favourites/Offers checks, Logout) completed successfully.");
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
