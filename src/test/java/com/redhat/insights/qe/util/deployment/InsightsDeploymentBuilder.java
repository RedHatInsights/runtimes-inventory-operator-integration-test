package com.redhat.insights.qe.util.deployment;

import cz.xtf.builder.builders.ApplicationBuilder;
import cz.xtf.client.Http;
import cz.xtf.core.bm.BinaryBuild;
import cz.xtf.core.bm.BuildManagers;
import cz.xtf.core.config.BuildManagerConfig;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.core.waiting.Waiter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class InsightsDeploymentBuilder {
  protected static final Logger logger = LogManager.getLogger(InsightsDeploymentBuilder.class);
  private final String appName;
  private final int replicas;
  private final BinaryBuild build;
  private final String hostname;
  private String urlSuffix;
  private final Map<String, String> deploymentEnvironmentVariables = Collections.emptyMap();
  private ApplicationBuilder appBuilder;

  private Waiter buildWaiter = null;
  private boolean buildFinished;

  private InsightsDeploymentBuilder(String appName, BinaryBuild build) {
    this.appName = appName;
    this.build = build;
    this.replicas = 1;
    this.hostname = OpenShifts.master().generateHostname(appName);
    this.urlSuffix = null;
  }

  public static InsightsDeploymentBuilder withApp(String appName, BinaryBuild build){
    return new InsightsDeploymentBuilder(appName, build);
  }

  public InsightsDeploymentBuilder urlCheck(final String urlSuffix) {
    this.urlSuffix = urlSuffix;
    return this;
  }

  private String checkUrl(){
    return this.urlSuffix != null ? "http://" + this.hostname + "/" + this.urlSuffix : null;
  }

  public InsightsDeploymentBuilder build() {
    checkUrlUnavailable(checkUrl());

    this.appBuilder = ApplicationBuilder.fromManagedBuild(this.appName, BuildManagers.get().getBuildReference(build));

    logger.info("Creating deployment config for {} with {} replicas.", this.appName, this.replicas);
    this.appBuilder.deploymentConfig(this.appName)
        .setReplicas(this.replicas)
        .podTemplate()
        .container()
        .envVars(this.deploymentEnvironmentVariables);
    this.appBuilder.service().port("http", 8080, 80);
    this.appBuilder.route();

    // fallback if build is not started via `@UsesBuild` annotation
    if (!build.isPresent(OpenShifts.master(BuildManagerConfig.namespace()))) {
      BuildManagers.get().deploy(build);
    }
    buildWaiter = BuildManagers.get().hasBuildCompleted(build).onFailure(() -> {
      throw new DeploymentException("Build " + build.getId() + " Failed");
    });
    buildFinished = false;

    return this;
  }

  public InsightsDeploymentBuilder waitForBuild(){
    if (buildWaiter == null){
      throw new DeploymentException("Waiting for build, which is not started");
    }
    buildWaiter.waitFor();
    buildFinished = true;

    return this;
  }

  public Waiter deploy() {
    if (!buildFinished){
      waitForBuild();
    }
    this.appBuilder.buildApplication(OpenShifts.master()).deploy();

    BooleanSupplier bs = () -> {
      OpenShifts.master().waiters().areExactlyNPodsReady(this.replicas, this.appName).waitFor();
      waitForCheckUrl(checkUrl());
      return true;
    };
    return new SimpleWaiter(bs, "Wait for application " + this.appName + " to deploy");
  }

  private void waitForCheckUrl(final String checkUrl) {
    if (checkUrl != null) {
      try {
        Http.get(checkUrl).trustAll().waiters().ok().waitFor();
      } catch (MalformedURLException e) {
        logger.error(e.getMessage());
      }
    }
  }

  private void checkUrlUnavailable(final String checkUrl) {
    if (checkUrl != null) {
      try {
        Assertions.assertEquals(503, Http.get(checkUrl).execute().code());
      } catch (final Exception x) {
        // ignore
      }
    }
  }
}
