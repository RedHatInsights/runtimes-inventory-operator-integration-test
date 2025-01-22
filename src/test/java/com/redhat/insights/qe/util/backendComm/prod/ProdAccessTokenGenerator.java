package com.redhat.insights.qe.util.backendComm.prod;


import com.redhat.insights.qe.util.backendComm.AbstractAccessTokenGenerator;

/**
 * Generates access token to prod environment.
 */
public class ProdAccessTokenGenerator extends AbstractAccessTokenGenerator {
    private static final String ssoEndpoint = "https://sso.redhat.com/auth/realms/redhat-external/protocol/openid-connect/token";

    private static final String ACCESS_TOKEN_CACHE_KEY = "PROD_ACCESS_TOKEN";

    public ProdAccessTokenGenerator() {
        super(ssoEndpoint, ACCESS_TOKEN_CACHE_KEY);
    }
}
