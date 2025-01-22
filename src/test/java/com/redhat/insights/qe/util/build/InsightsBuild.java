package com.redhat.insights.qe.util.build;

import cz.xtf.core.bm.BinaryBuild;
import cz.xtf.junit5.interfaces.BuildDefinition;
import com.redhat.insights.qe.TestConfig;

import java.util.Map;


public enum InsightsBuild implements BuildDefinition {
  SB3_JAR(new InsightsBuildDefinition("sb3-repackage-jar", "build-sb3-repackage-jar", "openjdk", Map.of("JAVA_APP_JAR", "spring-boot-3-app-jar-1.0.0.jar", "S2I_RUN_OPTS", "-javaagent:/deployments/" + TestConfig.getAgentFileName() + "=name=spring-boot-3-app-jar-1.0.0.jar;is_ocp=true;token=dummy;debug=true;base_url=" + TestConfig.getInsightsProxyUrl())));

  private final InsightsBuildDefinition buildDefinition;

  InsightsBuild(InsightsBuildDefinition buildDefinition) {
    this.buildDefinition = buildDefinition;
  }

  @Override
  public BinaryBuild getManagedBuild() {
    return buildDefinition.getManagedBuild();
  }
}
