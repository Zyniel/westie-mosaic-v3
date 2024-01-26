package com.zyniel.apps.westiemosaic.models.helpers;

import com.zyniel.apps.westiemosaic.enums.SupportedBrowsers;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

public class ConfigurationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationHelper.class);

    public static String getWestieAppUrl() {
        return "https://westie.app/";
    }

    public static String getLocalImageRepo() {
        return "C:\\images\\events";
    }

    public static SupportedBrowsers getBrowser() {
        return SupportedBrowsers.EDGE;
    }

    public static String getSessionData(SupportedBrowsers browserName) {
        String dir = "C:\\Users\\caban\\IdeaProjects\\westie-mosaic-v3\\";
        return Paths.get(dir, "Selenium", browserName.toString(), "User Data").toString();
        //String dir = System.getProperty("user.dir");
        //return Paths.get(dir, "Selenium", browserName.toString(), "User Data").toString();
    }

    public static String getUserEmail() {
        return "cabannes.francois.dance@gmail.com";
    }
}
