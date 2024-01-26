package com.zyniel.apps.westiemosaic.models;

import com.zyniel.apps.westiemosaic.models.helpers.ConfigurationHelper;
import com.zyniel.apps.westiemosaic.enums.ProcessingResult;
import com.zyniel.apps.westiemosaic.enums.RelativePosition;
import com.zyniel.apps.westiemosaic.models.helpers.SeleniumHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WestieParser implements IWestieParser{

    private static final Logger LOGGER = LoggerFactory.getLogger(WestieParser.class);

    final String MSG_SKIPPED_NOT_ELIGIBLE = "Skipping row: not an eligible event for processing";

    /** Processing engine used to parse the Website Events data.
     * Example :
     *  - WestieBasicParser: Extract images
     *  - WestieParser: Export Event data as JSON
     */
    private final ArrayList<EventProcessor> processors;

    /** Standard 10 s wait - Used by all standard checks */
    private WebDriverWait wait;
    /** Ultra quick millisecond wait - Used by EC checks */
    private WebDriverWait nowait;
    /** Ultra quick millisecond wait - Used for processing / heavy loadings checks */
    private WebDriverWait longwait;
    /** Ultra quick millisecond wait - Used for fast operations checks */
    private WebDriverWait shortwait;

    /**
     * Enumerates the main sections of the Westie.app website
     */
    private enum SiteSection {
        LOGIN_EMAIL, LOGIN_PIN, HOME, EVENTS, LESSONS, UNKNOWN, LOADING
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Constants used by Selectors to identify content of the Westie.app website
    //
    // NOTE: Works on 30/12/2023. Should be updated if necessary.
    // Will change due to app upgrade / changes, and break the workflow if not updated.
    // -----------------------------------------------------------------------------------------------------------------
    final String XPATH_EVENT_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Évènements à venir')]";
    final String XPATH_LESSONS_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Stages & Soirées')]";
    final String XPATH_HOME_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Accueil')]";
    final String XPATH_LOGIN_EMAIL_PAGE = "//div[@id='app-root']//form/input[@data-test='app-email-input']";
    final String XPATH_LOGIN_PIN_PAGE = "//div[@id='app-root']//form/input[@data-test='app-pin-input']";
    final String ID_LOADING = "loading-placeholder";
    final String XPATH_LOGIN_EMAIL_BTN = "//*[@id='app-root']/div[2]/div/div/div/div[3]/button[1]";
    final String XPATH_LOGIN_EMAIL_INPUT = "//div[@id='app-root']//form/input[@data-test='app-email-input']";
    final String XPATH_LOGIN_PIN_INPUT = "//div[@id='app-root']//form/input[@data-test='app-pin-input']";
    final String XPATH_HOME_EVENT_TILE = "//div[starts-with(@id, 'screenScrollView')]//div[@class='tile-title' and @data-test='tile-item-title' and contains(text(), 'Évènements')]";
    final String XPATH_EVENTS_LIST = "//div[starts-with(@id, 'screenScrollView')]//div[@data-test='app-vertical-list']/div[starts-with (@class, 'vlist___')]/div[starts-with (@class, 'vlist___')]/div[@data-index][.//div[contains(@class, 'tile-image-area')]]";
    final String XPATH_LAST_EVENTS_LIST = "(" + XPATH_EVENTS_LIST + ")[last()]";
    final String XPATH_EVENT_IDX_ATTR = "data-index";
    final String XPATH_EVENT_ROW = ".//div[@class='tile-inner']";
    final String XPATH_ROW_ELIGIBILITY = ".//div[contains(@class, 'tile-image-area')]";
    final String XPATH_APP_ROOT_DIV = "//div[starts-with(@id, 'screenScrollView')]";
    final String XPATH_VIEWPORT = "//div[starts-with(@id, 'OverlayscreenScrollView')]";
    final String XPATH_EVENT_FAVORITE = "//div[@data-test='app-toggle-icon-overlay']";
    final String XPATH_EVENTS_FILTERS = "//div[starts-with(@id, 'OverlayscreenScrollView') and @class='fab-target']";
    final int EVENT_SCROLL_TOP = 300;
    final int EVENT_SCROLL_LEFT = 0;

    private WebDriver driver;
    private String websiteUrl;

    private final int MAX_FLOW_RETRIES = 10;

    public WestieParser(String url) {
        // Create Edge Driver
        this.websiteUrl = url;
        this.processors = new ArrayList<>();
    }

    /**
     * @param processor to add to list of processors for data transformation
     */
    public void addProcessor(EventProcessor processor) {
        this.processors.add(processor);
    }

    /**
     * @param processor to remove from list of processors for data transformation
     */
    public void removeProcessor(EventProcessor processor) {
        this.processors.remove(processor);
    }

    /**
     * @param processor check if exists in the list of already registered processors
     * @return TRUE if found, FALSE if not yet registered
     */
    public boolean containsProcessor(EventProcessor processor) {
        return this.processors.contains(processor);
    }

    /**
     * @return The Selenium WebDriver used for parsing the Website
     */
    public WebDriver getDriver() {
        return this.driver;
    }

    /**
     * @param driver Define the Selenium WebDriver to use for parsing the Website
     */
    public void setDriver(WebDriver driver) {
        this.driver =  driver;
        this.longwait = new WebDriverWait(driver, Duration.ofSeconds(60));
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.shortwait = new WebDriverWait(driver, Duration.ofSeconds(1));
        this.nowait = new WebDriverWait(driver, Duration.ofMillis(10));
    }

    /**
     * @return URL to the Westie.App website
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * @param websiteUrl Define the URL to the Westie.App website
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Starts parsing Westie.App.
     * The parsing flow is the following: LOGIN via EMAIL > LOGIN by ENTERING PIN > Land on HOME page > Click EVENTS > Parse EVENTS
     * The parser starts by entering a Flow loop to identify the current page and the position in the flow to carry on.
     * <p>
     * In "LOGIN via EMAIL": inputs configured email address and presses "Sign In"
     * In "LOGIN by ENTERING PIN": waits x seconds for user to input the pin received by email and that he presses "Continue"
     * In "HOME Page": finds and clicks the Events tile
     * In "EVENTS": crawls the page for Event sections until it reaches the bottom of the page.
     * <p>
     * If the parser fails to start / restart the flow, it will fail and quit.
     * If the parser manages to reach Events and finish parsing, it will process all extracted events.
     *
     */
    public void parse() {
        LOGGER.info("Starting URL parsing...");

        try {
            // Instance new driver and waits
            WebDriver driver = SeleniumHelper.getSupportedBrowserDriver(ConfigurationHelper.getBrowser());
            setDriver(driver);
            try {
                SeleniumHelper.loadCookies(driver);
            } catch(IOException e) {
                LOGGER.warn("Could not load previously saved cookies.");
            }

            // Load Westie.app URL
            driver.get(websiteUrl);

            // Check for current opened page, to recover session flow
            // TODO: Understand how to handle arrays of generics ...
            LOGGER.info("Analysing current page");
            // Analyse current page to predict Worflow to follow
            // Using 'wait' to ensure the page has enough time to load
            boolean forceQuit = false;
            int curRetry = 0;
            int maxRetry = 10;
            SiteSection currentSection;
            do {
                currentSection = getCurrentSection();
                if (currentSection == SiteSection.UNKNOWN) {
                    LOGGER.error("Failed to identify page - waiting a bit more");
                    curRetry++;
                    forceQuit = (curRetry > maxRetry);
                    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
                } else if (currentSection == SiteSection.LOGIN_EMAIL) {
                    LOGGER.info("Login page found - No current session");
                    curRetry = 0;
                    loginUser();
                } else if (currentSection == SiteSection.LOGIN_PIN) {
                    LOGGER.info("PIN input page found - Ongoing Authentication");
                    curRetry = 0;
                } else if (currentSection == SiteSection.HOME) {
                    LOGGER.info("Landing page found - Already Authenticated");
                    curRetry = 0;
                    clickEvents();
                } else if (currentSection == SiteSection.EVENTS) {
                    LOGGER.info("Event page found - Already Authenticated");
                    curRetry = 0;
                    parseEvents();
                    forceQuit = true;
                } else if (currentSection == SiteSection.LESSONS) {
                    LOGGER.info("Lessons page found - Not implemented !");
                    curRetry = 0;
                    forceQuit = true;
                }
                // Wait is defered to getCurrentSection()
                // driver.manage().timeouts().implicitlyWait(Duration.ofMillis(100));
            } while (!forceQuit);

        } finally {
            try {
                SeleniumHelper.saveCookies(driver);
            } catch(IOException e) {
                LOGGER.warn("Could not save session cookies.");
            }
            driver.quit();
        }
        LOGGER.info("Finished URL parsing !");
    }

    /**
     * Attempts to find and click on the "Events" tile inside the HOME page
     */
    private void clickEvents() {
        LOGGER.info("Opening events...");
        SeleniumHelper.click(this.driver, By.xpath(XPATH_HOME_EVENT_TILE));
        LOGGER.info("Accessed events !");
    }

    /**
     * Attempts to parse Events inside the EVENTS page.
     * Does not manage filters and will take all events for now.
     * TODO: Manage filtering.
     */
    private void parseEvents() {
        LOGGER.debug("====== STARTING EVENT PARSER ======");
        int i = 1;
        int currentIdx = -1;
        String lastElementRef;
        String currentElementRef;

        // Attempt to retrieve the Event Viewport
        do {
            // Fetch visible events
            LOGGER.info(String.format(">>>>>>>>>> Loop %1d: Parsing visible events <<<<<<<<<<", i));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

            // Clean screen for parsing to ensure clean screenshots
            cleanupHUD();

            // For all registered processors
            for (EventProcessor processor : this.processors) {
                // Get all Event elements visible in the Selenium DOM as a list of WebElement then
                // Convert those elements to Jsoup Element for faster data parsing
                List<WebElement> eventWebElements = driver.findElements(By.xpath(XPATH_EVENTS_LIST));
                for (WebElement we : eventWebElements) {

                    // Lookup for eligible rows, containing and identifier and an image sections
                    currentIdx = checkIdentifier(we);
                    if (currentIdx != -1) {
                        Rectangle vpR = getViewport();
                        Rectangle evtR = new Rectangle(we.getLocation(), we.getRect().getDimension());
                        RelativePosition position = checkPosition(evtR, vpR);

                        // Check for processor specific short-circuits
                        if (processor.hasMore(position)) {
                            boolean processed = processor.process(we, position, currentIdx, true);
                            if (processor.lastResult == ProcessingResult.FAILED) {
                                LOGGER.error(MessageFormat.format("{0} - ({1})", processor.lastReason, processor.lastResult));
                            } else {
                                LOGGER.info(MessageFormat.format("{0} - ({1})", processor.lastReason, processor.lastResult));
                            }
                        } else {
                            LOGGER.info("No more event processing for this scroll.");
                            break;
                        }
                    } else {
                        LOGGER.info(MSG_SKIPPED_NOT_ELIGIBLE);
                    }
                }
            }

            lastElementRef = getLastRef();
            boolean scrollResult = SeleniumHelper.scrollElementBy(driver, By.xpath(XPATH_APP_ROOT_DIV), EVENT_SCROLL_TOP, EVENT_SCROLL_LEFT);
            currentElementRef = getLastRef();

            if (!lastElementRef.equals(currentElementRef)) {
                LOGGER.debug("Last element has changed > Scrolling");
            } else {
                LOGGER.debug("Last element has not changed > Finished Parsing");
            }
            i++;
        } while (!lastElementRef.equals(currentElementRef));

        LOGGER.debug("====== ENDING EVENT PARSER ======");
    }

    /**
     * Set of actions to log the user in as automatically input the Email, and waiting for manual PIN input.
     */
    private void loginUser() {
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_INPUT)));

        if (emailInput.isDisplayed()) {

            // Automatic user email input
            LOGGER.info("Login page found - Inputing Email");
            emailInput.sendKeys(ConfigurationHelper.getUserEmail());

            WebElement continueBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_BTN)));
            continueBtn.click();

            // Manual PIN input
            // TODO: Automate ? Rely on session ?
            WebElement pinInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_PIN_INPUT)));
            if (pinInput.isDisplayed()) {
                LOGGER.info("Manual PIN input needed - You have 60s to access mailbox and input PIN");
                // Wait until submission - when the PIN input is not found anymore
                longwait.until(ExpectedConditions.numberOfElementsToBe(By.xpath(XPATH_LOGIN_PIN_INPUT), 0));
                LOGGER.debug("Manual PIN input detected");
            } else {
                LOGGER.error("Authentication flow broken. Review app.");
            }
        }
    }

    /**
     * Removes unnecessary HUD elements overlapping event tiles and preventing a clean screenshot.
     */
    private void cleanupHUD() {
        // Remove hearths
        List<WebElement> hearthElements = driver.findElements(By.xpath(XPATH_EVENT_FAVORITE));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (WebElement hearth : hearthElements) {
            js.executeScript("arguments[0].style.visibility='hidden'", hearth);
        }

        // Remove hearths
        WebElement filtersElement = driver.findElement(By.xpath(XPATH_EVENTS_FILTERS));
        js.executeScript("arguments[0].style.visibility='hidden'", filtersElement);
    }

    /**
     * @return Corresponding SiteSection depending on the currently shown webpage if valid, else SiteSection.UNKNOWN.
     */
    private SiteSection getCurrentSection() {
        // Checks for any eligible pages
        boolean validSection = SeleniumHelper.anyElementsVisible(shortwait,
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_HOME_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_PIN_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_EVENT_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LESSONS_PAGE))
        );

        // Identify which section his currently shown
        if (validSection) {
            if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_HOME_PAGE))) {
                return SiteSection.HOME;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LOGIN_EMAIL_PAGE))) {
                return SiteSection.LOGIN_EMAIL;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LOGIN_PIN_PAGE))) {
                return SiteSection.LOGIN_PIN;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_EVENT_PAGE))) {
                return SiteSection.EVENTS;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LESSONS_PAGE))) {
                return SiteSection.LESSONS;
            } else {
                throw new RuntimeException("Expected section not found.");
            }
        } else {
            return SiteSection.UNKNOWN;
        }
    }

    /**
     * Attempt to parse the lookup and specific WebElement and return its Top Left corner position and dimension.
     * @return Viewport object containing TL corner position and WebElement dimensions
     */
    private Rectangle getViewport () {
        boolean isStale = false;
        int currentTry = 0;
        int maxRetry = 3;

        Rectangle rect = null;
        // Fetch viewport information
        do {
            try {
                WebElement vpElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_VIEWPORT)));
                rect = vpElement.getRect();
            } catch (StaleElementReferenceException e) {
                isStale = true;
                currentTry++;
            }
        } while (isStale && currentTry <= maxRetry);

        if (isStale || currentTry > maxRetry)  {
            throw new RuntimeException("Could not locate Viewport");
        }
        return new Rectangle(rect.getPoint(), rect.getDimension());
    }

    /**
     * Fetchs the last data-index (row number) of the paginated extraction currently visible
     * @return Integer representing the row number in the event list
     */
    private String getLastRef(){
        boolean isStale = false;
        int currentTry = 0;
        int maxRetry = 3;

        String ref = "";
        // Fetch last Index
        do {
            try {
                WebElement vpElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LAST_EVENTS_LIST)));
                ref = vpElement.getText();
            } catch (StaleElementReferenceException e) {
                isStale = true;
                currentTry++;
            }
        } while (isStale && currentTry <= maxRetry);

        if (isStale && currentTry > maxRetry)  {
            throw new RuntimeException("Could not locate last index");
        }
        return ref;
    }

    /**
     * Checks the vertical position of an "event" rectangle relatively to a second "viewport" one.
     * Event can be ABOVE, OVERLAPPING_TOP, INSIDE, OVERLAPPING_BOTTOM and BELOW "viewport".
     * Collision is considered inclusive.
     *
     * @param event First Rectangle, identified by a position and a dimension
     * @param viewport Second Rectangle, identified by a position and a dimension
     * @return a RelativePosition, ABOVE, OVERLAPPING_TOP, INSIDE, OVERLAPPING_BOTTOM and BELOW depending on "event"'s
     * position relatively to "viewport"
     */
    public RelativePosition checkPosition(Rectangle event, Rectangle viewport) {
        // Check if the event rectangle is above the viewport
        if (event.y + event.height <= viewport.y) {
            return RelativePosition.ABOVE;
        }

        // Check if the event rectangle is below the viewport
        if (event.y >= viewport.y + viewport.height) {
            return RelativePosition.BELOW;
        }

        // Check if the event rectangle is overlapping from the top
        if (event.y + event.height > viewport.y && event.y < viewport.y) {
            return RelativePosition.OVERLAPPING_TOP;
        }

        // Check if the event rectangle is overlapping from the bottom
        if (event.y < viewport.y + viewport.height && event.y + event.height > viewport.y + viewport.height) {
            return RelativePosition.OVERLAPPING_BOTTOM;
        }

        // If the event rectangle is neither above, below, nor overlapping, it must be fully contained within the viewport
        return RelativePosition.INSIDE;
    }

    /**
     * Takes a WebElement - representing an Event tile - and attempts to return the row ID of the event node.
     * @param we A parent WebElement to look into
     * @return -1 if not found else the ID as an integer.
     */
    private int checkIdentifier(WebElement we) {
        int currentIdx = -1;
        String identifier;
        try {
            // Only keep displayed rows with int ID - Remove technical ones
            identifier = we.getAttribute(XPATH_EVENT_IDX_ATTR);
            currentIdx = Integer.parseInt(identifier);
            // Only keep image events - Remove separators / dates
            WebElement image = we.findElement(By.xpath(XPATH_ROW_ELIGIBILITY));
        } catch(Exception e) {
            LOGGER.debug("Row is not eligible", e);
            return -1;
        }
        return currentIdx;
    }
}