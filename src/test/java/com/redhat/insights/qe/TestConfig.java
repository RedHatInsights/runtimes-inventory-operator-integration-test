package com.redhat.insights.qe;

import cz.xtf.core.config.XTFConfig;
import cz.xtf.core.image.Products;

public class TestConfig {

    public static String imageUrl(String product) {
        return Products.resolve(product).image().getUrl();
    }

    public static String getInsightsProxyUrl(){
        return XTFConfig.get("insights.proxy");
    }
    
    public static String getAgentUrl(){
        return XTFConfig.get("insights.agent");
    }

    public static String getOfflineToken() {
        return XTFConfig.get("insights.offline.token");
    }

    public static String getAgentFileName(){
        String agentUrl = getAgentUrl();
        String[] agentUrlParts = agentUrl.split("/");
        String agentFileName = agentUrlParts[agentUrlParts.length-1];        
        return agentFileName;
    }
}
