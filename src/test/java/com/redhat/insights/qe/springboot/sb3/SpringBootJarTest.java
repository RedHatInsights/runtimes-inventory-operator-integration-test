package com.redhat.insights.qe.springboot.sb3;

import com.redhat.insights.qe.util.build.InsightsBuild;
import com.redhat.insights.qe.util.deployment.InsightsDeploymentBuilder;
import com.redhat.insights.qe.springboot.AbstractSpringBootTest;
import org.junit.jupiter.api.BeforeAll;

public class SpringBootJarTest extends AbstractSpringBootTest {

    @BeforeAll
    public static void setupApplication() {
        InsightsDeploymentBuilder.withApp(APP_NAME, InsightsBuild.SB3_JAR.getManagedBuild())
                .urlCheck("/")
                .build()
                .deploy()
                .waitFor();
        appUrl = "http://" + openshift.generateHostname(APP_NAME);
    }
}
