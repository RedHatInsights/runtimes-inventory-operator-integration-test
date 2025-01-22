package com.redhat.insights.qe.util.exceptions;

public class BackendCommunicationException extends InsightTestException{
    public BackendCommunicationException() {
    }

    public BackendCommunicationException(String message) {
        super(message);
    }

    public BackendCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
