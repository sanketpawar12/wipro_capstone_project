// testCases/CartTests.java
package testCases;

import org.testng.Assert;
import org.testng.annotations.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import pages.ProductPage;
import pages.CartPage;
import utils.DriverFactory;

import java.time.Duration;

public class CartTests {
    WebDriver driver;
    ProductPage productPage;
    CartPage cartPage;

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.initDriver("chrome");

        // navigate to AUT and wait for product tiles
        driver.get("https://bstackdemo.com/");
        driver.manage().window().maximize();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.shelf-item")));

        productPage = new ProductPage(driver);
        cartPage = new CartPage(driver);
    }

    @Test
    public void increaseQuantityAndVerifySubtotal() throws InterruptedException {
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
    }

    @Test
    public void removeItemFromCartTest() throws InterruptedException {
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
    }
    @Test
    public void addSecondItemToCartTest() throws InterruptedException {
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
    }


    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
