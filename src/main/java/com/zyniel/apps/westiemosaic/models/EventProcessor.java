package com.zyniel.apps.westiemosaic.models;

import com.zyniel.apps.westiemosaic.enums.ProcessingResult;
import com.zyniel.apps.westiemosaic.enums.RelativePosition;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.text.MessageFormat;

public abstract class EventProcessor {

    protected ProcessingResult lastResult;
    protected String lastReason;

    /***
     * Process a single WebElement
     * @param eventTile WebElement representing the Westie.app Event DOM.
     *                  Contains a picture, event name, location, dates and a tag representing its nature
     * @param position Enum representing tile position relatively to the viewport. It allows visibility checks before
     *                 processing
     * @param idx Position index of the evenTile in the element list.
     * @return True is the parsing was fine, false is something did not work properly
     */
    public abstract boolean process(WebElement eventTile, RelativePosition position, int idx);

    /***
     * Process a single WebElement
     * @param eventTile WebElement representing the Westie.app Event DOM.
     *                  Contains a picture, event name, location, dates and a tag representing its nature
     * @param position Enum representing tile position relatively to the viewport. It allows visibility checks before
     *                 processing
     * @param idx Position index of the evenTile in the element list.
     * @param reset TRUE to signal a new processing and the need to reset some features, FALSE if the same
     * @return True is the parsing was fine, false is something did not work properly
     */
    public abstract boolean process(WebElement eventTile, RelativePosition position, int idx, boolean reset);

    protected void setResult(ProcessingResult result, String message, Object ... placeholders) {
        this.lastResult = result;
        this.lastReason = MessageFormat.format(message, placeholders);
        // getLogger().info(lastReason);
    }

    /**
     * @return class logger
     */
    protected abstract Logger getLogger();

    /***
     * @return BEFORE if the event is bellow not yet visible, INSIDE if visible, AFTER if under and not yet visible
     */
    public abstract boolean hasMore(RelativePosition position);

    /**
     * Resets process internal features
     */
    protected abstract void resetBatch();
}
