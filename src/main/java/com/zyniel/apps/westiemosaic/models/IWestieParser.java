package com.zyniel.apps.westiemosaic.models;

import org.openqa.selenium.WebDriver;

public interface IWestieParser {
    /***
     * Starts Westie.app scrapping and data parsing
     */
    void parse();

    WebDriver getDriver();
}
