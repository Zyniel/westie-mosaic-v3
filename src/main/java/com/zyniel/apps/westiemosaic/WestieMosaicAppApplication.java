package com.zyniel.apps.westiemosaic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WestieMosaicAppApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WestieMosaicAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WestieMosaicAppApplication.class, args);
    }

}