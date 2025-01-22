package com.redhat.insights.qe.util.build;

import com.redhat.insights.qe.TestConfig;
import com.redhat.insights.qe.TestParent;
import cz.xtf.core.bm.BinaryBuild;
import cz.xtf.core.bm.BinarySourceBuild;
import cz.xtf.junit5.interfaces.BuildDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Libor Fuka (lfuka@redhat.com)
 */
public class InsightsBuildDefinition implements BuildDefinition {
  private final static String MODULE_NAME = "insights";

  private String buildName;
  private Path path;
  private String product;  
  private Map<String, String> envProperties;

  private BinarySourceBuild managedBuild = null;

  public InsightsBuildDefinition(String appName, String buildName, String product, Map<String, String> envProperties) {
    init(buildName, TestParent.findApplicationDirectory(appName, MODULE_NAME), product, envProperties);
  }

  private void init(String buildName, Path path, String product, Map<String, String> envProperties) {
    this.buildName = buildName;
    this.path = path;
    this.product = product;
    this.envProperties = envProperties;
  }

  @Override
  public BinaryBuild getManagedBuild() {
    if (managedBuild == null) {
      try {
        Path preparedSources = TestParent.prepareProjectSources(this.buildName, this.path, this.product);

        Map<String, String> properties = new HashMap<>();

        if (this.envProperties != null) {
          properties.putAll(this.envProperties);
        }

        managedBuild = new BinarySourceBuild(TestConfig.imageUrl(this.product), preparedSources, properties, this.buildName);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return managedBuild;
  }
}
