package com.redhat.insights.qe.springboot;

import com.redhat.insights.qe.TestParent;
import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractSpringBootTest extends TestParent {
    protected static final String APP_NAME = "sb-3-app-jar";
    protected static String appUrl;

    @Test
    public void helloWorldTest() throws Exception {
        String response = httpGetResponse(appUrl);
        Assertions.assertThat(response).isEqualTo("Greetings from Spring Boot!");
    }

    @Test
    public void workLoadTypeTest() {
        //"workloadType" : "Spring Boot"
        waitForPodLogToContain(APP_NAME, "\"workloadType\" : \"Spring Boot\"");
    }

    @Test
    public void insightsPayloadAcceptedTest() {
        //Pod log must contain: Red Hat Insights - Payload was accepted for processing
        waitForPodLogToContain(APP_NAME, "Red Hat Insights - Payload was accepted for processing");
    }

    @Test
    public void reportIsUploadedTest() throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException {
        checkForUploadedReport(APP_NAME, "Spring Boot");
    }
}
