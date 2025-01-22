package com.redhat.insights.qe.util.backendComm;

import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AccessTokenGenerator {
    String getAccessToken(String offlineToken) throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException;
}
