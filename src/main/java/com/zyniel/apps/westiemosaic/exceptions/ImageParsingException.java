package com.zyniel.apps.westiemosaic.exceptions;

public class ImageParsingException extends Exception{
    public ImageParsingException(String errorMessage) {
        super(errorMessage);
    }
    public ImageParsingException(String errorMessage, Exception err) {
        super(errorMessage, err);
    }
}
