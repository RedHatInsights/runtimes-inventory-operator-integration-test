package com.redhat.insights.qe.util.exceptions;

public class InsightTestException extends Exception{
    public InsightTestException() {
        super();
    }

    public InsightTestException(String message) {
        super(message);
    }

    public InsightTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
