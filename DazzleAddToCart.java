package tests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DazzleAddToCart {

    // ---------- Locators ----------
    static By SEARCH_BOX = By.cssSelector("input[placeholder='Search for the item']");
    static By RESULT_PRODUCT = By.cssSelector("a[href^='/product/']");
    static By PRODUCT_TITLE = By.tagName("h1");
    static By ADD_TO_CART = By.xpath("//button[normalize-space()='Add to cart']");
    static By ADDED_BTN = By.xpath("//button[contains(normalize-space(),'Added')]");
    static By CART_LINK = By.cssSelector("a[aria-label='Cart']");

    static By QTY_INPUT = By.xpath("//input[preceding-sibling::button and following-sibling::button] | //input[@type='number']");
    static By QTY_TEXT = By.xpath("//*[normalize-space(text())='1' and preceding-sibling::button and following-sibling::button]");

    static WebElement firstVisible(WebDriver d, By by) {
        for (WebElement e : d.findElements(by)) {
            try {
                if (e.isDisplayed()) return e;
            } catch (Exception ignored) { }
        }
        return null;
    }

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(6));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // 218. Open the Dazzle website
            driver.get("https://dazzle.com.bd/");
            driver.manage().window().maximize();
            System.out.println("Step 218: Opened -> " + driver.getTitle());

            // 219. Search for a smartphone
            boolean searched = false;
            for (int attempt = 0; attempt < 3 && !searched; attempt++) {
                try {
                    wait.until(d -> firstVisible(d, SEARCH_BOX) != null);
                    js.executeScript("arguments[0].focus(); arguments[0].click();", firstVisible(driver, SEARCH_BOX));
                    firstVisible(driver, SEARCH_BOX).sendKeys("smartphone");
                    firstVisible(driver, SEARCH_BOX).sendKeys(Keys.ENTER);
                    shortWait.until(ExpectedConditions.urlContains("search"));
                    searched = true;
                } catch (Exception ex) {
                    System.out.println("   Search retry " + (attempt + 1) + ": " + ex.getClass().getSimpleName());
                    driver.get("https://dazzle.com.bd/");
                }
            }
            if (!searched) {
                // fallback: go directly to the search URL
                driver.get("https://dazzle.com.bd/search?q=smartphone");
            }
            wait.until(ExpectedConditions.urlContains("search"));
            System.out.println("Step 219: Searched -> " + driver.getCurrentUrl());

            // 220. Open a product from the search results
            wait.until(ExpectedConditions.presenceOfElementLocated(RESULT_PRODUCT));
            List<String> hrefs = new ArrayList<>();
            for (WebElement e : driver.findElements(RESULT_PRODUCT)) {
                String h = e.getAttribute("href");
                if (h != null && !hrefs.contains(h)) hrefs.add(h);
            }
            String productName = null;
            WebElement addBtn = null;

            for (int i = 0; i < hrefs.size() && i < 6 && addBtn == null; i++) {
                driver.get(hrefs.get(i));
                wait.until(d -> firstVisible(d, PRODUCT_TITLE) != null);
                productName = firstVisible(driver, PRODUCT_TITLE).getText().trim();
                try {
                    shortWait.until(d -> firstVisible(d, ADD_TO_CART) != null);
                    addBtn = firstVisible(driver, ADD_TO_CART);
                } catch (TimeoutException te) {
                    System.out.println("   Skip (no Add to cart button): " + productName);
                }
            }
            if (addBtn == null) throw new RuntimeException("Could not find an Add to cart button on any product");
            System.out.println("Step 220: Opened product -> " + productName);

            // 221. Click the Add to Cart button
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", addBtn);
            js.executeScript("arguments[0].click();", addBtn);
            wait.until(ExpectedConditions.presenceOfElementLocated(ADDED_BTN));
            System.out.println("Step 221: Clicked Add to Cart -> Added! confirmed");

            // 222. Open the shopping cart
            wait.until(d -> firstVisible(d, CART_LINK) != null);
            js.executeScript("arguments[0].click();", firstVisible(driver, CART_LINK));
            wait.until(ExpectedConditions.urlContains("/cart"));
            System.out.println("Step 222: Cart opened -> " + driver.getCurrentUrl());

            // 223. Verify the selected product is displayed in the cart
            final String key = productName.toLowerCase();
            boolean inCart;
            try {
                wait.until(d -> d.findElement(By.tagName("body")).getText().toLowerCase().contains(key));
                inCart = true;
            } catch (TimeoutException te) {
                inCart = false;
            }
            System.out.println(inCart ? "Step 223 PASS: '" + productName + "' exists in cart"
                                      : "Step 223 FAIL: '" + productName + "' not found in cart");

            // 224. Verify the cart quantity is one
            String qty = "NOT FOUND";
            String method = "";
            WebElement q = firstVisible(driver, QTY_INPUT);
            WebElement t = firstVisible(driver, QTY_TEXT);
            String body = driver.findElement(By.tagName("body")).getText();
            if (q != null && q.getAttribute("value") != null && !q.getAttribute("value").isEmpty()) {
                qty = q.getAttribute("value").trim();
                method = "quantity input";
            } else if (t != null) {
                qty = t.getText().trim();
                method = "quantity text";
            } else if (body.matches("(?is).*Shopping Cart\\s*\\(1\\).*")) {
                qty = "1";
                method = "cart heading (1)";
            }
            System.out.println("Cart quantity = " + qty + "  [method: " + method + "]");
            System.out.println("1".equals(qty) ? "Step 224 PASS: Quantity is one"
                                               : "Step 224 FAIL: Quantity is not one");

        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}