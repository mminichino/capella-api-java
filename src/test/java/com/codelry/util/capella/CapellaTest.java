package com.codelry.util.capella;

import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import com.couchbase.client.core.env.PasswordAuthenticator;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.ScopeExistsException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapellaTest {
  private static final Logger LOGGER = LogManager.getLogger(CapellaTest.class);
  public String profileName = "pytest";
  public String projectName = "pytest-project";
  public String clusterName = "pytest-cluster";
  public String bucketName = "data";
  public String scopeName = "group";
  public String collectionName = "table";
  public String allowedCIDR = "0.0.0.0/0";
  public String username = "developer";
  public String password = "#C0uchBas3";
  public static CapellaProject project;
  public static CapellaCluster cluster;
  public static CapellaBucket bucket;
  public static CapellaAllowedCIDR cidr;
  public static CapellaCredentials user;

  @Test
  public void testCapella1() {
    CouchbaseCapella capella = CouchbaseCapella.getInstance(projectName, profileName);
    CapellaOrganization organization = CapellaOrganization.getInstance(capella);
    project = CapellaProject.getInstance(organization);
    Assertions.assertNotNull(project.getId());
  }

  @Test
  public void testCapella2() {
    cluster = CapellaCluster.getInstance(project);
    CapellaCluster.ClusterBuilder clusterBuilder = new CapellaCluster.ClusterBuilder();
    clusterBuilder.clusterName(clusterName);
    cluster.createCluster(clusterBuilder);
  }

  @Test
  public void testCapella3() {
    Assertions.assertNotNull(cluster);
    cidr = CapellaAllowedCIDR.getInstance(cluster);
    cidr.createAllowedCIDR(allowedCIDR);
  }

  @Test
  public void testCapella4() {
    Assertions.assertNotNull(cluster);
    user = CapellaCredentials.getInstance(cluster);
    user.createCredential(username, password, new ObjectMapper().createArrayNode());
  }

  @Test
  public void testCapella5() {
    Assertions.assertNotNull(cluster);
    bucket = CapellaBucket.getInstance(cluster);
    BucketSettings bucketSettings = BucketSettings.create(bucketName).ramQuotaMB(128);
    bucket.createBucket(bucketSettings);
  }

  @Test
  public void testCapella6() {
    Assertions.assertNotNull(cluster);
    String connectString = cluster.getConnectString();
    PasswordAuthenticator authenticator = PasswordAuthenticator.create(username, password);
    Consumer<SecurityConfig.Builder> secConfiguration;
    secConfiguration = securityConfig -> securityConfig
        .enableTls(true)
        .enableHostnameVerification(false)
        .trustManagerFactory(InsecureTrustManagerFactory.INSTANCE);
    ClusterEnvironment environment = ClusterEnvironment
        .builder()
        .securityConfig(secConfiguration)
        .build();
    try (Cluster cluster = Cluster.connect(connectString, ClusterOptions.clusterOptions(authenticator).environment(environment))) {
      cluster.waitUntilReady(Duration.ofSeconds(15));
      Bucket bucket = cluster.bucket(bucketName);
      CollectionManager collectionManager = bucket.collections();
      try {
        collectionManager.createScope(scopeName);
      } catch (ScopeExistsException e) {
        LOGGER.debug("Scope {} already exists in cluster", scopeName);
      }
      try {
        collectionManager.createCollection(scopeName, collectionName);
      } catch (CollectionExistsException e) {
        LOGGER.debug("Collection {} already exists in cluster", collectionName);
      }
    }
  }

  @Test
  public void testCapella7() {
    Assertions.assertNotNull(bucket);
    bucket.delete();
  }

  @Test
  public void testCapella8() {
    Assertions.assertNotNull(cluster);
    cluster.delete();
  }
}
