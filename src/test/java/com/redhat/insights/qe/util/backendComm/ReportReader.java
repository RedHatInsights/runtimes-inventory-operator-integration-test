package com.redhat.insights.qe.util.backendComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface ReportReader {
    void setAccessToken(String accessToken);

    JsonNode getReport(String reportId) throws IOException, URISyntaxException, InterruptedException, BackendCommunicationException;

    List<String> getAllReportIds(String hostname) throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException;
}
