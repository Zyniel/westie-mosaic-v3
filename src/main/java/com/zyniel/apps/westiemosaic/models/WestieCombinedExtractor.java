package com.zyniel.apps.westiemosaic.models;

import com.zyniel.apps.westiemosaic.entities.WestieEvent;
import com.zyniel.apps.westiemosaic.entities.WestieSocialEvent;
import com.zyniel.apps.westiemosaic.entities.WestieWSDCEvent;
import com.zyniel.apps.westiemosaic.exceptions.DataParsingException;
import com.zyniel.apps.westiemosaic.exceptions.ImageParsingException;
import com.zyniel.apps.westiemosaic.enums.ProcessingResult;
import com.zyniel.apps.westiemosaic.enums.RelativePosition;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


public class WestieCombinedExtractor extends EventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WestieCombinedExtractor.class);

    // ===========================================
    // ====== PROCESSING & RESULT MESSAGES =======
    // ===========================================
    final String MSG_SUCCESSFUL_IMAGE = "Image was extracted";
    final String MSG_SUCCESSFUL_DATA = "Event was extracted";
    final String MSG_SKIPPED_NOT_VISIBLE_YET = "Event is not yet visible in viewport";
    final String MSG_SKIPPED_NOT_FULLY_VISIBLE_YET = "Element is not yet fully visible in viewport";
    final String MSG_SKIPPED_NOT_VISIBLE_ANYMORE = "Element is not visible anymore in viewport";
    final String MSG_SKIPPED_NOT_FULLY_VISIBLE_ANYMORE = "Element is not fully visible anymore in viewport";
    final String MSG_PROCESSING_INSIDE = "Element is visible";
    final String MSG_FAILURE_SAVING_DATA = "Failed to save event data";
    final String MSG_FAILURE_SAVING_IMAGE = "Failed to save image";
    final String MSG_FAILURE_CREATE_PNG = "Failed to create .png from base64";
    final String MSG_SKIPPED_IMAGE_ALREADY_EXISTS = "Image already extracted";
    final String MSG_SKIPPED_DATA_ALREADY_EXISTS = "Data already extracted";
    final String MSG_OVERALL_RESULT = "Result: {0} - Data: {1} - Image: {2}";

    // ===========================================
    // ====== XPATH EXPRESSIONS FOR PARSING ======
    // ===========================================
    final String XPATH_EVENT_FAVORITE = "//div[@data-test='app-toggle-icon-overlay']";
    final String XPATH_IMG_TAG = "img";
    final String XPATH_EVENTS_FILTERS = "//div[starts-with(@id, 'OverlayscreenScrollView') and @class='fab-target']";
    final String XPATH_EVENT_IMAGE = ".//div[contains(@class,'tile-image-area')]";
    final String CONF_DEFAULT_IMAGE_DIR = "./static/images/events/";

    private final Rectangle viewport = null;

    private IWestieParser parser;

    private final HashMap<Integer, String> eventB64Images;
    private final HashMap<Integer, WestieEvent> eventInfos;
    private ProcessingResult lastDataResult;
    private String lastDataReason;
    private ProcessingResult lastImageResult;
    private String lastImageReason;

    public WestieCombinedExtractor(IWestieParser parser) {
        this.parser = parser;
        this.eventB64Images = new HashMap<>();
        this.eventInfos = new HashMap<>();
        lastDataResult = ProcessingResult.NOT_STARTED;
        lastImageResult = ProcessingResult.NOT_STARTED;
        lastDataReason = "";
        lastImageReason = "";
    }

    @Override
    public boolean process(WebElement eventTile, RelativePosition position, int idx, boolean reset) {
        if (reset) {
            resetBatch();
        }
        return process(eventTile, position, idx);
    }

    @Override
    protected Logger getLogger() {
        return WestieCombinedExtractor.LOGGER;
    }

    @Override
    public boolean hasMore(RelativePosition position) {
        return position != RelativePosition.BELOW && position != RelativePosition.OVERLAPPING_BOTTOM;
    }

    @Override
    protected void resetBatch() {
        //
    }

    /***
     * Returns a base64 encoded string representing a picture of the event tile taken using a Selenium Screenshot.
     * Only visible events, present in the Viewport range can be extracted.
     * @param eventTile Event tile which needs capture
     * @param position Event position relative to the viewport to assert capture possibility
     * @param idx Event index in the list
     * @throws ImageParsingException Custom Exception dedicated to image processing
     */
    public void extractAndSaveImage(WebElement eventTile, RelativePosition position, int idx) throws ImageParsingException {
        // Only process visible tiles as the screenshot engine cannot manage hidden / out of viewport images
        if (position == RelativePosition.INSIDE) {
            LOGGER.info(formatLogMessage(idx, "Processing event: " + MSG_PROCESSING_INSIDE));
            WebElement imageTile = eventTile.findElement(By.xpath(XPATH_EVENT_IMAGE));
            String b64Image = extractB64Image(imageTile, idx);
            eventB64Images.put(idx, b64Image);
            setImageResult(ProcessingResult.SUCCESSFUL, formatLogMessage(idx, MSG_SUCCESSFUL_DATA));
        } else if (position != null) {
            setImageResult(ProcessingResult.SKIPPED,
                    switch (position) {
                        case RelativePosition.ABOVE -> MSG_SKIPPED_NOT_VISIBLE_YET;
                        case RelativePosition.OVERLAPPING_TOP -> MSG_SKIPPED_NOT_FULLY_VISIBLE_YET;
                        case RelativePosition.OVERLAPPING_BOTTOM -> MSG_SKIPPED_NOT_VISIBLE_ANYMORE;
                        case RelativePosition.BELOW -> MSG_SKIPPED_NOT_FULLY_VISIBLE_ANYMORE;
                        case RelativePosition.INSIDE -> "";
                    }
                    , Integer.toString(idx));
        }
    }

    /***
     * Transforms data contained in an event tile into a WestieEvent object.
     * No visibility condition for data parsing.
     * @param eventTile Event tile which needs capture
     * @param position Event position relative to the viewport to assert capture possibility
     * @param idx Event index in the list
     * @throws DataParsingException Custom Exception dedicated to data processing
     */
    public void extractAndSaveData(WebElement eventTile, RelativePosition position, int idx) throws DataParsingException {
        // Process any tile independently of position
        if (position != null) {
            WestieEvent eventObject = extractEventData(eventTile, idx);
            eventInfos.put(idx, eventObject);
        }
    }

    /***
     * Processing of an event tile, combining event information extraction and image extraction.
     * If successful, each extraction step result will be saved once by ID.
     */
    public boolean process(WebElement eventTile, RelativePosition position, int idx) {
        boolean imageExtracted = false;
        boolean dataExtracted = false;
        this.lastResult = ProcessingResult.NOT_STARTED;
        this.lastReason = "";
        this.lastDataResult = ProcessingResult.NOT_STARTED;
        this.lastDataReason = "";
        this.lastImageResult = ProcessingResult.NOT_STARTED;
        this.lastImageReason = "";

        // Only extract and save image once per ID
        if (eventB64Images.containsKey(idx)) {
            setImageResult(ProcessingResult.SKIPPED, formatLogMessage(idx, MSG_SKIPPED_IMAGE_ALREADY_EXISTS));
            imageExtracted = true;
        } else {
            try {
                extractAndSaveImage(eventTile, position, idx);
                // setImageResult(ProcessingResult.SUCCESSFUL, formatLogMessage(idx, MSG_SUCCESSFUL_IMAGE));
                imageExtracted = true;
            } catch (ImageParsingException e) {
                LOGGER.error("ImageProcessingException", e);
                setImageResult(ProcessingResult.FAILED, formatLogMessage(idx, MSG_FAILURE_SAVING_IMAGE));
            }
        }
        // LOGGER.info("... Image > " + this.lastImageResult + " " + this.lastImageReason);

        // Only extract and save data once per ID
        if (eventInfos.containsKey(idx)) {
            setDataResult(ProcessingResult.SKIPPED, formatLogMessage(idx, MSG_SKIPPED_DATA_ALREADY_EXISTS));
            dataExtracted = true;
        } else {
            try {
                extractAndSaveData(eventTile, position, idx);
                // setDataResult(ProcessingResult.SUCCESSFUL, formatLogMessage(idx, MSG_SUCCESSFUL_DATA));
                dataExtracted = true;
            } catch (DataParsingException e) {
                LOGGER.error("DataProcessingException", e);
                setDataResult(ProcessingResult.FAILED, formatLogMessage(idx, MSG_FAILURE_SAVING_IMAGE));
            }
        }

        if (this.lastDataResult == ProcessingResult.SKIPPED && this.lastImageResult == ProcessingResult.SKIPPED) {
            this.lastResult = ProcessingResult.SKIPPED;
        } else if (this.lastDataResult == ProcessingResult.SUCCESSFUL || this.lastImageResult == ProcessingResult.SUCCESSFUL) {
            this.lastResult = ProcessingResult.SUCCESSFUL;
        } else if (this.lastDataResult == ProcessingResult.FAILED || this.lastImageResult == ProcessingResult.FAILED){
            this.lastResult = ProcessingResult.FAILED;
        } else {
            this.lastResult = ProcessingResult.FAILED;
        }

        String strResult = StringUtils.rightPad(this.lastResult.toString(), 11);
        String strDataResult = StringUtils.rightPad(this.lastDataResult.toString(), 11);
        String strImageResult = StringUtils.rightPad(this.lastImageResult.toString(), 11);

        setResult(this.lastResult, formatLogMessage(idx, MessageFormat.format(MSG_OVERALL_RESULT, strResult, strDataResult, strImageResult)));
        return imageExtracted || dataExtracted;
    }

    /***
     * Takes a Selenium WebElement representing an event tile, extractions the outer HTML using JSOup and extract chunks
     * of information to create a WestieEvent model object.
     *
     * @param eventTile WebElement representing the Westie.app Event DOM.
     *                  Contains a picture, event name, location, dates and a tag representing its nature
     * @param idx Position index of the evenTile in the element list.
     * @return A model object containing the event information
     * @throws DataParsingException Any failure to attempt extraction (Selenium crashes, Exceptions ...)
     */
    private WestieEvent extractEventData(WebElement eventTile, int idx) throws DataParsingException {
        // Extract WebElement HTML for faster and stale-free processing
        Document doc = Jsoup.parse(eventTile.getAttribute("outerHTML"));
        Element elt = doc.getElementsByAttribute("data-index").first();
        WestieEvent evt = null;
        if (!eventInfos.containsKey(idx)) {
            try {
                evt = convertToEvent(elt, idx);
                setDataResult(ProcessingResult.SUCCESSFUL, formatLogMessage(idx, MSG_SUCCESSFUL_DATA));
            } catch (RuntimeException | ParseException | MalformedURLException | URISyntaxException e) {
                setDataResult(ProcessingResult.FAILED, formatLogMessage(idx, MSG_FAILURE_SAVING_DATA));
            }
        } else {
            setDataResult(ProcessingResult.SKIPPED, formatLogMessage(idx, MSG_SKIPPED_IMAGE_ALREADY_EXISTS));
        }
        return evt;
    }

    /***
     * Takes a Selenium WebElement representing an event tile, take a screenshot of the tile before and return a base64
     * representation of the picture.
     *
     * @param eventTile WebElement representing the Westie.app Event DOM.
     *                  Contains a picture, event name, location, dates and a tag representing its nature
     * @param idx Position index of the evenTile in the element list.
     * @return A model object containing the event information
     * @throws ImageParsingException Any failure to attempt extraction (Selenium crashes, Exceptions ...)
     */
    private String extractB64Image(WebElement eventTile, int idx) throws ImageParsingException {
        try {
            return eventTile.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            throw new ImageParsingException("Failed to convert the event tile to a base64 string.", e);
        }
    }

    private WestieEvent convertToEvent(Element element, int idx) throws RuntimeException, ParseException, MalformedURLException, URISyntaxException {
        if (element != null) {
            // XPath Expressions for elements
            String xpathImgOverlay = "//div[contains(@class,'tile-overlay')]";
            String xpathImgTop = xpathImgOverlay + "/div[contains(@class,'tile-corner-container')]";
            String xpathImgTopLeft = xpathImgTop + "/div[contains(@class,'top-left-content') and contains(@class,'corner-content')]";
            String xpathImgTopLeftTag = xpathImgTopLeft + "/div[@data-test='app-tag-overlay']";
            String xpathImgBottom = xpathImgOverlay + "/div[contains(@class,'tile-corner-container')]/div[contains(@class,'bottom-left-content') and contains(@class,'corner-content')]";
            String xpathImgCenter = xpathImgOverlay + "/div[contains(@class,'center-content') and contains(@class,'corner-content')]/div[contains(@class,'tile-text-container')]";
            // Get RAW data
            String title = element.selectXpath(xpathImgCenter + "/div[contains(@class,'tile-title')]").text();
            String subtitle = element.selectXpath(xpathImgCenter + "/div[contains(@class,'tile-subtitle')]").text();
            String dates = element.selectXpath(xpathImgBottom).text();
            String tag = element.selectXpath(xpathImgTopLeftTag).text();

            // Convert data
            // Split Subtitle by the ',''
            // -> Left  : City
            // -> Right : Country
            int sepIdx = subtitle.indexOf(',');
            String city = subtitle.substring(0, sepIdx).trim();
            String country = subtitle.substring(sepIdx + 1).trim();

            // WSDC event if the tag exists and contains WSDC
            boolean isWSDC = (tag.equalsIgnoreCase("WSDC"));

            // Event name is the Title
            String name = title;

            // Cannot define at time of parsing
            String location = "";

            String bannerUrl = "";
            String facebookUrl = "";
            String websiteUrl = "";
            String imageFile = idx + ".png";

            // ---------------------------------------------------------------------------------------------------------
            // Date parsing will rely on three different patterns based on event period being
            // ---------------------------------------------------------------------------------------------------------
            // Split the date section by '-'
            // -> Pattern 01 : 2 dates on single month - dd-dd MMMM yyyy
            //      + Start : First 'dd' + " " + Right side 'MMMM yyyy '
            //      + End   : RHS of  pattern 'dd MMMM yyyy'
            // -> Pattern 02 : 2 dates over two different months - 'dd MMMM yyyy - dd MMMM yyyy'
            //      + Start : LHS of pattern 'dd MMMM yyyy'
            //      + End   : RHS of pattern 'dd MMMM yyyy'
            // -> Pattern 03 : 2 dates over different months in different years 'dd MMMM - dd MMMM yyyy'
            //      + Start : LHS of pattern 'dd MMMM' + ' ' + RHS of pattern 'yyyy'
            //      + End   : RHS of pattern 'dd MMMM yyyy'

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
            String rawEndDate = "";
            String rawStartDate = "";
            Date endDate = null;
            Date startDate = null;
            sepIdx = dates.indexOf('-');

            Pattern sameMonth = Pattern.compile("\\d{1,2}-\\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);
            Pattern sameYear = Pattern.compile("\\d{1,2} \\w+ - \\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);
            Pattern diffYears = Pattern.compile("\\d{1,2} \\w+ \\d{4} - \\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);

            boolean isSameMonth = sameMonth.matcher(dates).matches();
            boolean isSameYear = sameYear.matcher(dates).matches();
            boolean isDiffYears = diffYears.matcher(dates).matches();

            // Pattern 01: "12-14 Janvier 2024"
            if (isSameMonth || isSameYear || isDiffYears) {
                if (isSameMonth) {
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    rawStartDate = dates.substring(0, sepIdx).trim() + " " + rawEndDate.substring(2).trim();
                    LOGGER.debug("Pattern 01: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                // Pattern 02: "12 Janvier - 14 Février 2024"
                else if (isSameYear) {
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    rawStartDate = dates.substring(0, sepIdx - 1) + " " + rawEndDate.substring(rawEndDate.length() - 4);
                    LOGGER.debug("Pattern 02: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                // Pattern 03: "31 Décembre 2023 - 03 Janvier 2024"
                else {
                    rawStartDate = dates.substring(0, sepIdx).trim();
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    LOGGER.debug("Pattern 03: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                try {
                    startDate = formatter.parse(rawStartDate);
                } catch (ParseException e) {
                    LOGGER.error("Unable to parse the Start Date: " + rawStartDate, e);
                }
                try {
                    endDate = formatter.parse(rawEndDate);
                } catch (ParseException e) {
                    LOGGER.error("Unable to parse the End Date: " + rawEndDate, e);
                }
            } else {
                throw new RuntimeException("Unknown date pattern");
            }

            WestieEvent evt = null;
            if (isWSDC) {
                evt = new WestieWSDCEvent(name, startDate, endDate, city, country);
                evt.setBannerUrl(bannerUrl);
                evt.setImageFile(imageFile);
            } else {
                evt = new WestieSocialEvent(name, startDate, endDate, city, country);
                evt.setBannerUrl(bannerUrl);
                evt.setImageFile(imageFile);
            }
            return evt;
        } else {
            throw new RuntimeException("Failed to convert Element to WestieEvent");
        }
    }

    private void cleanupHUD(Wait<WebDriver> wait) {

        WebDriver driver = this.parser.getDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Remove filters
        WebElement filtersElement = driver.findElement(By.xpath(XPATH_EVENTS_FILTERS));
        js.executeScript("arguments[0].style.visibility='hidden'", filtersElement);

        // Remove favorites
        List<WebElement> hearthElements = driver.findElements(By.xpath(XPATH_EVENT_FAVORITE));
        for (WebElement hearth : hearthElements) {
            js.executeScript("arguments[0].style.visibility='hidden'", hearth);
        }

        // Remove visual search on images
        List<WebElement> imgElements = driver.findElements(By.tagName(XPATH_IMG_TAG));
        for (WebElement img : imgElements) {
            js.executeScript("arguments[0].style.pointerEvents = 'none';", img);
        }
    }

    protected void setImageResult(ProcessingResult result, String message, Object... placeholders) {
        this.lastImageResult = result;
        this.lastImageReason = MessageFormat.format(message, placeholders);
    }

    protected void setDataResult(ProcessingResult result, String message, Object... placeholders) {
        this.lastDataResult = result;
        this.lastDataReason = MessageFormat.format(message, placeholders);
    }


    /**
     * @return a map of event banner images encoded in base64 and indexed by event id
     */
    public HashMap<Integer, String> getEventB64Images() {
        return eventB64Images;
    }

    /**
     * @return a map of WestieEvent indexed by event id
     */
    public HashMap<Integer, WestieEvent> getEventInfos() {
        return eventInfos;
    }

    /**
     * Log formatter to track row id along with log message during he whole parsing process.
     * @param idx Tile index
     * @param message Text message to trace
     * @return A formatted string
     */
    private String formatLogMessage(int idx, String message) {
        return MessageFormat.format("Tile {0}: {1}", String.format("%1$4s", idx).replace(' ', '0'), message);
    }

}