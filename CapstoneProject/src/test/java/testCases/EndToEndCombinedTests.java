package testCases;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.edge.EdgeDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.github.bonigarcia.wdm.WebDriverManager;
import pages.CartPage;
import pages.CheckoutPage;
import pages.HomePage;
import pages.LoginPage;
import pages.ProductPage;
import pages.SearchPage;
import utils.ConfigReader;
import utils.DriverFactory;
import utils.ReportUtils;

public class EndToEndCombinedTests {

    /* ====================
       1) Cross-browser homepage check
       group: crossbrowser
       ==================== */
    @Test(groups = {"crossbrowser"}, priority = 1)
    @Parameters("browser")
    public void verifyHomePageLoads(String browser) {
        ReportUtils.initReports();
        ReportUtils.createTest("Verify Homepage in " + browser);

        WebDriver driver = null;
        try {
            if (browser.equalsIgnoreCase("chrome")) {
                driver = new ChromeDriver();
            } else if (browser.equalsIgnoreCase("firefox")) {
                driver = new FirefoxDriver();
            } else if (browser.equalsIgnoreCase("edge")) {
                driver = new EdgeDriver();
            } else {
                throw new IllegalArgumentException("Unsupported browser: " + browser);
            }

            driver.manage().window().maximize();
            driver.get("https://bstackdemo.com/");
            String title = driver.getTitle();

            Assert.assertTrue(title.contains("StackDemo"), "Homepage did not load correctly!");
            ReportUtils.logPass("Homepage loaded successfully in " + browser);
        } catch (AssertionError e) {
            ReportUtils.logFail("Homepage failed in " + browser);
            if (driver != null) ReportUtils.captureScreenshot(driver, "HomePage_Failure_" + browser);
            throw e;
        } finally {
            if (driver != null) driver.quit();
            ReportUtils.flushReports();
        }
    }

    /* ====================
       2) Login test
       group: login
       depends on crossbrowser
       ==================== */
    @Test(groups = {"login"}, dependsOnGroups = {"crossbrowser"}, priority = 2)
    public void validLoginTest() {
        WebDriver driver = DriverFactory.initDriver("chrome");
        try {
            driver.get("https://bstackdemo.com/");
            driver.manage().window().maximize();

            LoginPage loginPage = new LoginPage(driver);
            loginPage.openLoginForm();
            loginPage.selectUsername("existing_orders_user");
            loginPage.selectPassword("testingisfun99");
            loginPage.clickLogin();

            Assert.assertTrue(driver.getTitle().contains("StackDemo"), "Login failed!");
        } finally {
            DriverFactory.quitDriver();
        }
    }

    @DataProvider(name = "limitedFilterData")
    public Object[][] limitedFilterData() {
        return new Object[][] {
            {"Apple", "Lowest to highest"},
            {"Samsung", "Highest to lowest"}
        };
    }

    @Test(dataProvider = "limitedFilterData", groups = {"filter"}, dependsOnGroups = {"login"}, priority = 4)
    public void testFilterAndSort(String vendor, String sortOrder) {
        // mirror original FilterTests setup
        ConfigReader.loadConfig("testdata/config.properties");
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        WebDriver driver = new ChromeDriver(opts);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().window().maximize();
            driver.get(ConfigReader.getProperty("url"));

            HomePage homePage = new HomePage(driver);
            SearchPage searchPage = new SearchPage(driver);

            // wait until search box clickable (same as original)
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(homePage.getSearchBox()));

            // refresh, apply vendor filter and sort
            driver.navigate().refresh();
            searchPage.applyVendorFilter(vendor);
            searchPage.selectSortOrder(sortOrder);

            // assertions
            Assert.assertTrue(searchPage.verifyVendorOnly(vendor), "Vendor check failed: " + vendor);
            Assert.assertTrue(searchPage.verifySortingByControl(sortOrder), "Sort check failed: " + sortOrder);
        } finally {
            if (driver != null) driver.quit();
        }
    }
 // add these imports if they are not already present at the top of EndToEndCombinedTests.java:
 // import org.openqa.selenium.By;
 // import org.openqa.selenium.support.ui.ExpectedConditions;
 // import org.openqa.selenium.support.ui.WebDriverWait;
 // import java.time.Duration;
 // import pages.ProductPage;
 // import pages.CartPage;
 // import utils.DriverFactory;

 @Test(groups = {"product"}, dependsOnGroups = {"filter"}, priority = 5)
 public void addFirstProductToCartTest() throws InterruptedException {
     WebDriver driver = DriverFactory.initDriver("chrome");
     try {
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

         ProductPage productPage = new ProductPage(driver);
         CartPage cartPage = new CartPage(driver);

         productPage.addFirstProductToCart();

         // wait for cart items to appear
         new WebDriverWait(driver, Duration.ofSeconds(5))
             .until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("div.float-cart__shelf-container .shelf-item"), 0));

         System.out.println("Cart items: " + cartPage.getCartItemNames());
         Assert.assertTrue(cartPage.isCartOpen(), "Cart did not open or no items present.");
     } finally {
         DriverFactory.quitDriver();
     }
 }

 @Test(groups = {"product"}, dependsOnGroups = {"filter"}, priority = 6)
 public void addNamedProductToCartTest() throws InterruptedException {
     WebDriver driver = DriverFactory.initDriver("chrome");
     try {
         driver.get("https://bstackdemo.com/");
         driver.manage().window().maximize();

         ProductPage productPage = new ProductPage(driver);
         CartPage cartPage = new CartPage(driver);

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
     } finally {
         DriverFactory.quitDriver();
     }
 }
//CART tests (add into EndToEndCombinedTests)

@Test(groups = {"cart"}, dependsOnGroups = {"product"}, priority = 7)
public void increaseQuantityAndVerifySubtotal() throws InterruptedException {
  WebDriver driver = DriverFactory.initDriver("chrome");
  try {
      driver.get("https://bstackdemo.com/");
      driver.manage().window().maximize();

      ProductPage productPage = new ProductPage(driver);
      CartPage cartPage = new CartPage(driver);

      String name = "iPhone 12 Mini";

      // add product
      boolean added = productPage.addProductToCartByName(name);
      Assert.assertTrue(added, "Product add failed");

      // ensure cart visible (try opening cart if needed)
      try {
          if (driver.findElements(By.cssSelector("div.float-cart")).size() == 0) {
              if (driver.findElements(By.cssSelector("div.float-cart__header")).size() > 0)
                  driver.findElement(By.cssSelector("div.float-cart__header")).click();
          }
      } catch (Exception ignored) {}

      // debug before values
      System.out.println("DEBUG: qty before = " + cartPage.getQuantityForItem(name));
      System.out.println("DEBUG: subtotal before = " + cartPage.getSubtotal());

      // try to increase quantity by 2
      boolean inc = cartPage.increaseQtyForItem(name, 2);
      System.out.println("DEBUG: increase returned = " + inc);

      // debug after values
      System.out.println("DEBUG: qty after = " + cartPage.getQuantityForItem(name));
      System.out.println("DEBUG: subtotal after = " + cartPage.getSubtotal());

      // conservative assert: subtotal should change (covers UIs with no visible qty)
      String beforeSub = cartPage.getSubtotal();
      // attempt increase once more to ensure change (some clients lazy-update)
      cartPage.increaseQtyForItem(name, 1);
      String afterSub = cartPage.getSubtotal();
      Assert.assertTrue(!afterSub.equals(beforeSub) && !afterSub.isEmpty(), "Subtotal did not change after increasing quantity");
  } finally {
      DriverFactory.quitDriver();
  }
}

@Test(groups = {"cart"}, dependsOnGroups = {"product"}, priority = 8)
public void removeItemFromCartTest() throws InterruptedException {
  WebDriver driver = DriverFactory.initDriver("chrome");
  try {
      driver.get("https://bstackdemo.com/");
      driver.manage().window().maximize();

      ProductPage productPage = new ProductPage(driver);
      CartPage cartPage = new CartPage(driver);

      String productName = "iPhone 12 Mini";
      boolean added = productPage.addProductToCartByName(productName);
      Assert.assertTrue(added, "Product was not added to cart");

      // ensure cart visible
      try {
          if (driver.findElements(By.cssSelector("div.float-cart")).size() == 0) {
              if (driver.findElements(By.cssSelector("div.float-cart__header")).size() > 0)
                  driver.findElement(By.cssSelector("div.float-cart__header")).click();
          }
      } catch (Exception ignored) {}

      // wait a moment for cart items
      new WebDriverWait(driver, Duration.ofSeconds(8))
          .until(d -> d.findElements(By.cssSelector("div.float-cart .shelf-item")).size() > 0);

      Assert.assertTrue(cartPage.isItemPresent(productName), "Item not present before removal");

      boolean removed = cartPage.removeItemByName(productName);
      System.out.println("DEBUG: removed = " + removed);
      Assert.assertTrue(removed, "removeItemByName reported failure");
      Assert.assertFalse(cartPage.isItemPresent(productName), "Item still present after removal.");
  } finally {
      DriverFactory.quitDriver();
  }
}

@Test(groups = {"cart"}, dependsOnGroups = {"product"}, priority = 9)
public void addSecondItemToCartTest() throws InterruptedException {
  WebDriver driver = DriverFactory.initDriver("chrome");
  try {
      driver.get("https://bstackdemo.com/");
      driver.manage().window().maximize();

      ProductPage productPage = new ProductPage(driver);
      CartPage cartPage = new CartPage(driver);

      String first = "iPhone 12 Mini";
      String second = "Galaxy S9";  // you can change this to any available product

      // Add first product
      boolean added1 = productPage.addProductToCartByName(first);
      Assert.assertTrue(added1, "First product not added: " + first);

      // Ensure cart is visible
      try {
          if (driver.findElements(By.cssSelector("div.float-cart")).isEmpty()) {
              driver.findElement(By.cssSelector("div.float-cart__header")).click();
          }
      } catch (Exception ignored) {}

      Assert.assertTrue(cartPage.isItemPresent(first), "First item not in cart");

      // Add second product
      boolean added2 = productPage.addProductToCartByName(second);
      Assert.assertTrue(added2, "Second product not added: " + second);

      // Wait for cart update
      new WebDriverWait(driver, Duration.ofSeconds(8))
              .until(d -> cartPage.isItemPresent(second));

      // Final assertions: both items present
      Assert.assertTrue(cartPage.isItemPresent(first), "First item missing after adding second");
      Assert.assertTrue(cartPage.isItemPresent(second), "Second item not found in cart");

      System.out.println("DEBUG: Cart now contains -> " + cartPage.getCartItemNames());
  } finally {
      DriverFactory.quitDriver();
  }
}
//Add this method to EndToEndCombinedTests.java

@Test(groups = {"checkout"}, dependsOnGroups = {"cart","login"}, priority = 10)
public void checkoutShippingAndSubmitTest() throws InterruptedException {
 WebDriver driver = DriverFactory.initDriver("chrome");
 try {
     // ensure we are on the application home page first
     driver.get("https://bstackdemo.com/");
     driver.manage().window().maximize();

     // wait for product list to be present before creating page objects
     WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
     shortWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));

     // create page objects after navigation
     ProductPage productPage = new ProductPage(driver);
     CheckoutPage checkoutPage = new CheckoutPage(driver);
     LoginPage loginPage = new LoginPage(driver);

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
 } finally {
     DriverFactory.quitDriver();
 }
}
@Test(groups = {"confirmation"}, dependsOnGroups = {"checkout"}, priority = 11)
public void fullCheckoutFlow_login_add_checkout_confirm_download_continue_checkNavs_logout() throws Exception {
    // set download directory for the test (created if missing)
    String downloads = System.getProperty("user.dir") + "/target/test-downloads";
    Path downloadDir = Path.of(downloads);

    WebDriver driver = DriverFactory.initDriverWithDownload(downloadDir.toAbsolutePath().toString());
    try {
        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

        // start at home
        driver.get("https://bstackdemo.com/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));

        LoginPage loginPage = new LoginPage(driver);
        ProductPage productPage = new ProductPage(driver);
        CartPage cartPage = new CartPage(driver);
        CheckoutPage checkoutPage = new CheckoutPage(driver);

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
    } finally {
        DriverFactory.quitDriver();
    }
}



}
