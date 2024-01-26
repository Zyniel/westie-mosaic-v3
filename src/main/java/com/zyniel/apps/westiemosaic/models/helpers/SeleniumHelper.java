package com.zyniel.apps.westiemosaic.models.helpers;

import com.zyniel.apps.westiemosaic.enums.SupportedBrowsers;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class SeleniumHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumHelper.class);

    // Comparing value as switch
    public static WebDriver getSupportedBrowserDriver(SupportedBrowsers browserName) {
        WebDriver driver = null;
        String dataFolder = System.getenv("LOCALAPPDATA");

        switch (browserName) {
            case CHROME:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("start-maximized"); // open Browser in maximized mode
                chromeOptions.addArguments("disable-infobars"); // disabling infobars
                chromeOptions.addArguments("--disable-extensions"); // disabling extensions
                chromeOptions.addArguments("--disable-gpu"); // applicable to Windows os only
                chromeOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
                chromeOptions.addArguments("--no-sandbox"); // Bypass OS security model
                chromeOptions.addArguments("--disable-in-process-stack-traces");
                chromeOptions.addArguments("--disable-logging");
                chromeOptions.addArguments("--log-level=3");
                chromeOptions.addArguments("--remote-allow-origins=*");

                // Save sessions
                String googleDataFolder = dataFolder + "\\Google\\Chrome\\User Data\\";
                chromeOptions.addArguments("--user-data-dir=" + googleDataFolder);
                LOGGER.debug(googleDataFolder);

                driver = WebDriverManager
                        .chromedriver()
                        .capabilities(chromeOptions)
                        .create();

            case EDGE:
                // Set options
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--headless");
                edgeOptions.addArguments("profile-directory=Default");
                edgeOptions.addArguments("start-maximized"); // open Browser in maximized mode
                edgeOptions.addArguments("--disable-infobars"); // disabling infobars
                edgeOptions.addArguments("--disable-extensions"); // disabling extensions
                edgeOptions.addArguments("--disable-gpu"); // applicable to Windows os only
                edgeOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
                edgeOptions.addArguments("--no-sandbox"); // Bypass OS security model
                edgeOptions.addArguments("--disable-in-process-stack-traces");
                edgeOptions.addArguments("--disable-logging");
                edgeOptions.addArguments("--log-level=3");
                edgeOptions.addArguments("--remote-allow-origins=*");
                // edgeOptions.addArguments("--guest");
                // edgeOptions.addArguments("--inprivate");

                // Set preferences
                HashMap<String, Object> edgePrefs = new HashMap<>();
                edgePrefs.put("user_experience_metrics.personalization_data_consent_enabled", 0);
                edgePrefs.put("profile.default_content_settings.popups", 0);
                edgePrefs.put("profile.default_content_setting_values.notifications", 2);
                edgePrefs.put("profile.default_content_setting_values.automatic_downloads" , 1);
                edgePrefs.put("profile.content_settings.pattern_pairs.*,*.multiple-automatic-downloads",1);
                edgePrefs.put("profile.record_user_choices.show_greeting", 1);
                edgePrefs.put("profile.record_user_choices.show_image_of_day", 0);
                edgePrefs.put("profile.record_user_choices.show_settings", 0);
                edgePrefs.put("profile.record_user_choices.show_top_sites", 0);
                // Remove all fluff on Homepage to Load faster
                edgePrefs.put("ntp.background_image_type", "off");                  // ---> Remove background
                edgePrefs.put("ntp.background_image.provider", "NoBackground");     //
                edgePrefs.put("ntp.background_image.userSelected", 1);              // <--- Remove background
                edgePrefs.put("ntp.layout_mode", 0);                                // ---> Remove all content
                edgePrefs.put("ntp.news_feed_display", "headingsonly");             //
                edgePrefs.put("ntp.num_personal_suggestions", 0);                   //
                edgePrefs.put("ntp.quick_links_options", 0);                        //
                edgePrefs.put("ntp.record_user_choices.setting", "layout_mode");    //
                edgePrefs.put("ntp.record_user_choices.source", "ntp");             //
                edgePrefs.put("ntp.record_user_choices.value", 0);                  //
                edgePrefs.put("ntp.show_greeting", 0);                              //
                edgePrefs.put("ntp.show_image_of_day", 0);                          //
                edgePrefs.put("ntp.show_settings", 0);                              // <--- Remove all content

                // Remove Smart Explore & Visual Search
                edgePrefs.put("smart_explore.on_image_hover", "false");
                edgePrefs.put("smart_explore.block_list", Arrays.asList("westie.app"));
                edgePrefs.put("visual_search.dma_state", "1");
                edgePrefs.put("visual_search.in_context_menu", "false");

                // Remove Sync
                edgePrefs.put("sync.in_context_menu.preferences", "false");
                edgePrefs.put("sync.has_been_enabled", "false");
                edgePrefs.put("sync.has_setup_completed", "true");
                edgePrefs.put("sync_consent_recorded", "true");

                edgeOptions.setExperimentalOption("prefs", edgePrefs);
                edgeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

                // Change Page Load Strategy
                edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

                // Save sessions
                String edgeDataFolder = ConfigurationHelper.getSessionData(browserName);
                edgeOptions.addArguments("user-data-dir=" + edgeDataFolder);
                edgeOptions.addArguments("profile-directory=WestieMosaic");
                LOGGER.debug(edgeDataFolder);

                // Setup Driver
                driver = WebDriverManager
                        .edgedriver()
                        .capabilities(edgeOptions)
                        .create();
        }

        return driver;
    }

    public static void click(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
    }

    public static void type(WebDriver driver, By locator, String text) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
        element.clear();
        element.sendKeys(text);
    }

    public static  Boolean isElementVisible(Wait<WebDriver> wait, By elementBy) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(elementBy));
        } catch (NoSuchElementException | TimeoutException e) {
            LOGGER.debug(Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    public static Boolean anyElementsVisible(Wait<WebDriver> wait, ExpectedCondition<?>... elementsBy) {
        try {
            wait.until(ExpectedConditions.or(elementsBy));
        } catch (NoSuchElementException | TimeoutException e) {
            LOGGER.debug(Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    public static boolean elementIsSelected(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        return element.isSelected();
    }

    public static String getText(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(locator));
        return element.getText();
    }

    public static boolean scrollToPageBottom(WebDriver driver) {
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        return (initialLength == currentLength);
    }

    public static boolean scrollToPageTop(WebDriver driver) {
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        return (initialLength == currentLength);
    }

    public static boolean scrollElementBy(WebDriver driver, By by, int top, int left) {
        // Get height before
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(by));
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", element);

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy({ top: " + top + ", left: " + left +", behavior: \"smooth\" });", element);

        // Get height after and compare
        element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(by));
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", element);

        return (initialLength == currentLength);
    }

    public static void saveCookies(WebDriver driver) throws IOException {
        // Get all cookies
        var cookies = driver.manage().getCookies();
        var path = Paths.get("Cookies.data");

        try (var writer = Files.newBufferedWriter(path)) {
            for (var cookie : cookies) {
                writer.write(cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() + ";" + cookie.getPath() + ";" + cookie.getExpiry() + ";" + cookie.isSecure());
                writer.newLine();
            }
        }
    }

    public static void loadCookies(WebDriver driver) throws IOException {
        var path = Paths.get("Cookies.data");

        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                String line;

                while ((line = reader.readLine()) != null) {
                    var token = new StringTokenizer(line, ";");
                    while (token.hasMoreTokens()) {
                        var name = token.nextToken();
                        var value = token.nextToken();
                        var domain = token.nextToken();
                        var pathToken = token.nextToken();
                        Date expiry = null;

                        var expiryVal = token.nextToken();
                        if (!expiryVal.equals("null")) {
                            expiry = new Date(expiryVal);
                        }
                        var isSecure = Boolean.parseBoolean(token.nextToken());
                        var cookie = new Cookie(name, value, domain, pathToken, expiry, isSecure);
                        driver.manage().addCookie(cookie); // This will add the stored cookie to your current session
                    }
                }
            }
        }
    }
}
