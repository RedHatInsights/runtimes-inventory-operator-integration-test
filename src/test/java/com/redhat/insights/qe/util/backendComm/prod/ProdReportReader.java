package com.redhat.insights.qe.util.backendComm.prod;

import com.redhat.insights.qe.util.backendComm.AbstractReportReader;

import java.net.http.HttpClient;

public class ProdReportReader extends AbstractReportReader {
    private static final String PROD_REST_ENDPOINT_PREFIX = "https://console.redhat.com/api/runtimes-inventory-service/v1/";

    public ProdReportReader() {
        super(PROD_REST_ENDPOINT_PREFIX, HttpClient.newHttpClient());
    }
}
