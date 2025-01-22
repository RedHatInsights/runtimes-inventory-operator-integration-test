package com.redhat.insights.qe.util;

import com.redhat.insights.qe.util.backendComm.ReportReader;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Put it one place checks, that appears on lot of places
 */
public class CommonChecks {
    public static void expectNumberOfReportsOnBackend(ReportReader reportReader, String hostname, int expectedCount, String failureMessage){
        await()
                .atMost(Duration.ofSeconds(5))
                .alias(failureMessage)
                .until(() -> reportReader.getAllReportIds(hostname).size() == expectedCount);
    }
}
