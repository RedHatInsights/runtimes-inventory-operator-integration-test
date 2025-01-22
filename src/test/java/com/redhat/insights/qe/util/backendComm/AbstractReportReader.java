package com.redhat.insights.qe.util.backendComm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReportReader implements ReportReader {
    protected final HttpClient httpClient;
    protected final String restEndpointPrefix;
    protected String accessToken;

    public AbstractReportReader(String restEndpointPrefix, HttpClient httpClient) {
        this.restEndpointPrefix = restEndpointPrefix;
        this.httpClient = httpClient;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public List<String> getAllReportIds(String hostname) throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException {
        String response = performRequest("instance-ids/?hostname="+hostname);
        return reportIdsJsonToList(response);
    }

    public JsonNode getReport(String reportId) throws IOException, URISyntaxException, InterruptedException, BackendCommunicationException {
        String response = performRequest("instance/?jvmInstanceId=" + reportId);

        JsonMapper mapper = new JsonMapper();
        JsonNode jsonRoot = mapper.readTree(response);

        return jsonRoot.get("response");
    }

    private String performRequest(String uriSuffix) throws BackendCommunicationException, IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder(new URI(restEndpointPrefix + uriSuffix))
                .GET()
                .header("Authorization", "Bearer " + accessToken)
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200){
            throw new BackendCommunicationException("Https request against stage failed, status code: " + response.statusCode());
        }

        return response.body();
    }

    private List<String> reportIdsJsonToList(String stageResponseBody) throws JsonProcessingException {
        JsonMapper mapper = new JsonMapper();
        JsonNode response = mapper.readTree(stageResponseBody);

        List<String> result = new ArrayList<>();

        response.get("response").forEach(element -> result.add(element.asText()));

        return result;
    }
}
