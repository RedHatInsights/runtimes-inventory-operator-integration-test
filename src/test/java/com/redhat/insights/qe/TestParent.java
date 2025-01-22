package com.redhat.insights.qe;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.insights.qe.util.CommonChecks;
import com.redhat.insights.qe.util.backendComm.AccessTokenGenerator;
import com.redhat.insights.qe.util.backendComm.ReportReader;
import com.redhat.insights.qe.util.backendComm.prod.ProdAccessTokenGenerator;
import com.redhat.insights.qe.util.backendComm.prod.ProdReportReader;
import com.redhat.insights.qe.util.exceptions.BackendCommunicationException;
import cz.xtf.client.Http;
import cz.xtf.client.HttpResponseParser;
import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.junit5.annotations.OpenShiftRecorder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@OpenShiftRecorder
public abstract class TestParent {
    protected static final Logger logger = LogManager.getLogger(TestParent.class);
    protected static final OpenShift openshift = OpenShifts.master();
    protected static final Path TMP_SOURCES_DIR = Paths.get("tmp").toAbsolutePath().resolve("sources");
    protected static final Path INSIGHTS_AGENT_DIR = Paths.get("agent").toAbsolutePath();
    protected static final String AGENT_FILE_NAME = TestConfig.getAgentFileName();

    protected static final AccessTokenGenerator accessTokenGenerator = new ProdAccessTokenGenerator();
    protected static final ReportReader reportReader = new ProdReportReader();
    protected static String offlineToken;
    protected List<String> reports;

    protected static String httpGetResponse(String url) throws IOException {
        return httpGet(url).response();
    }

    protected static HttpResponseParser httpGet(String url) throws IOException {
        return Http.get(url).trustAll().execute();
    }

    public static Path prepareProjectSources(final String appName, final Path projectDir, final String product) throws IOException {
        if (projectDir == null) {
            return null;
        }

        Files.createDirectories(TMP_SOURCES_DIR);
        Path sourcesDir = Files.createTempDirectory(TMP_SOURCES_DIR.toAbsolutePath(), appName);
        FileUtils.copyDirectory(projectDir.toFile(), sourcesDir.toFile());

        //copy insights agent from INSIGHTS_AGENT_DIR to TMP_SOURCES_DIR/appName/deployments directory for OpenJDK image
        //copy insights agent from INSIGHTS_AGENT_DIR to TMP_SOURCES_DIR/appName/lib directory for JWS image
        //copy insights agent from INSIGHTS_AGENT_DIR to TMP_SOURCES_DIR/appName/modules directory for EAP image
        String s2iAgentDirectory;
        if (product.equals("jws")) {
            s2iAgentDirectory = "lib";
        } else if (product.equals("eap7") || product.equals("eap8")) {
            s2iAgentDirectory = "modules";
        } else {
            s2iAgentDirectory = "deployments";
        }

        Path deploymentsDir = Files.createDirectory(sourcesDir.resolve(s2iAgentDirectory).toAbsolutePath());
        FileUtils.copyFile(new File(INSIGHTS_AGENT_DIR.toString() + "/" + AGENT_FILE_NAME),
                sourcesDir.resolve(s2iAgentDirectory).resolve(AGENT_FILE_NAME).toFile());

        return sourcesDir;
    }

    public static Path findApplicationDirectory(String appName, String appModuleName) {
        if (appModuleName != null) {
            Path path = FileSystems.getDefault().getPath("src/test/resources/apps/" + appModuleName, appName);
            if (Files.exists(path)) {
                return path;
            }
            logger.info("Path {} does not exist", path.toAbsolutePath());
        }
        throw new IllegalArgumentException("Cannot find directory with STI app sources");
    }


    public static void downloadInsightsAgent() throws IOException {
        //download insights agent to INSIGHTS_AGENT_DIR if not already exists
        File sourceAgentFile = new File(INSIGHTS_AGENT_DIR.toString() + "/" + AGENT_FILE_NAME);

        if (!sourceAgentFile.exists()) {
            URL agentUrl = new URL(TestConfig.getAgentUrl());

            if (agentUrl.getProtocol().startsWith("http")) {
                int CONNECTION_TIMEOUT_MILLLIS = Long.valueOf(TimeUnit.SECONDS.toMillis(60)).intValue();
                int READ_TIMEOUT_MILLLIS = Long.valueOf(TimeUnit.SECONDS.toMillis(60)).intValue();

                // create directory only if it does not exist
                Path deploymentsDir = Files.createDirectories(INSIGHTS_AGENT_DIR);
                FileUtils.copyURLToFile(agentUrl, sourceAgentFile, CONNECTION_TIMEOUT_MILLLIS, READ_TIMEOUT_MILLLIS);

                if (sourceAgentFile.length() == 0) {
                    throw new RuntimeException("Insights agent was not downloaded successfully.");
                }
            }
            // if agent file is local, just copy it
            else if (agentUrl.getProtocol().startsWith("file")){
                Files.copy(Path.of(agentUrl.getPath()), sourceAgentFile.toPath());
            }
        }
    }

    /**
     * Check that exactly one report was uploaded to the prod backend
     *
     * @param deploymentName Name of the deployment on OCP - will search for its pod name
     * @param workload       Substring of expected workload type to be found in the report
     */
    protected void checkForUploadedReport(String deploymentName, String workload) throws URISyntaxException, IOException, InterruptedException, BackendCommunicationException {
        // initialize the reportReader
        String accessToken = accessTokenGenerator.getAccessToken(offlineToken);
        reportReader.setAccessToken(accessToken);

        // reports are identified with podName, so get this
        String podName = openshift.getAnyPod(deploymentName).getMetadata().getName();

        // wait for report to be uploaded
        CommonChecks.expectNumberOfReportsOnBackend(reportReader, podName, 1,
                "There should be one report uploaded");

        // get the actual report from the backend
        List<String> newReportIds = reportReader.getAllReportIds(podName);
        JsonNode report = reportReader.getReport(newReportIds.get(0));
        reports.add(report.toPrettyString());

        // verify workload in the report
        assertTrue(report.has("workload"), "Report should have field \"workload\"");
        assertTrue(report.get("workload").asText().contains(workload), "Report workload should contain string: " + workload);
    }

    protected void waitForPodLogToContain(String appName, String stringToContain) {
        Awaitility.await().atMost(Duration.ofSeconds(5L)).alias("Pod log does not contain expected message: " + stringToContain)
                .until(() -> openshift.getPodLog(appName).contains(stringToContain));
    }

    @BeforeEach
    public void resetReports() {
        reports = new ArrayList<>();
    }

    @BeforeAll
    public static void cleanProjectBeforeClass() throws IOException {
        openshift.clean().waitFor();

        offlineToken = TestConfig.getOfflineToken();
        assumeFalse(offlineToken == null, "Require offline token to be set.");

        //download insights agent only once
        downloadInsightsAgent();
    }

    @AfterAll
    public static void cleanupAfterClass() {
        openshift.clean().waitFor();
    }
}
