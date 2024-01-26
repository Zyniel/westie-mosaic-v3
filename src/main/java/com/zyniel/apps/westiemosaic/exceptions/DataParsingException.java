package com.zyniel.apps.westiemosaic.exceptions;

public class DataParsingException extends Exception{
    public DataParsingException(String errorMessage) {
        super(errorMessage);
    }
    public DataParsingException(String errorMessage, Exception err) {
        super(errorMessage, err);
    }
}
