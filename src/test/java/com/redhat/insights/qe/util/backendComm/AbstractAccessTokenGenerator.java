package com.redhat.insights.qe.util.backendComm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAccessTokenGenerator implements AccessTokenGenerator {
    protected static final Logger logger = LogManager.getLogger(AbstractAccessTokenGenerator.class);

    private final String ssoEndpoint;
    protected final HttpClient httpClient;
    protected static LoadingCache<String, String> cache = null;
    private final String accessTokenCacheKey;

    public AbstractAccessTokenGenerator(String ssoEndpoint, String tokenCacheKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.accessTokenCacheKey = tokenCacheKey;
        this.ssoEndpoint = ssoEndpoint;
        initCache();
    }

    public void initCache(){
        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(4, TimeUnit.MINUTES)
                    .removalListener(notification -> logger.debug("Entry removed from cache: " + notification.getKey()))
                    .build(new CacheLoader<>() {
                        @Override
                        public String load(String key) {
                            return key;
                        }
                    });
        }
    }

    public String getAccessToken(String offlineToken) throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException {
        String accessToken = cache.getIfPresent(accessTokenCacheKey);
        if (accessToken != null) {
            return accessToken;
        }
        accessToken = retrieveAccessToken(offlineToken);
        logger.debug("Retrieved new access token");
        cache.put(accessTokenCacheKey, accessToken);
        return accessToken;
    }

    /**
     * Get access token from stage
     */
    private String retrieveAccessToken(String offlineToken) throws BackendCommunicationException, IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder(new URI(ssoEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        getFormDataAsString(
                                createTokenRequestParams(offlineToken))))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new BackendCommunicationException("Getting access token failed, got status code: " + response.statusCode() + ". Response body: " + response.body());
        }

        return parseToken(response.body());
    }

    private Map<String, String> createTokenRequestParams(String offlineToken) {
        Map<String, String> params = new HashMap<>();

        params.put("grant_type", "refresh_token");
        params.put("client_id", "rhsm-api");
        params.put("refresh_token", offlineToken);

        return params;
    }

    private String parseToken(String serverResponse) throws JsonProcessingException {
        JsonMapper mapper = new JsonMapper();
        JsonNode response = mapper.readTree(serverResponse);

        return response.get("access_token").asText();
    }

    private String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }
}
