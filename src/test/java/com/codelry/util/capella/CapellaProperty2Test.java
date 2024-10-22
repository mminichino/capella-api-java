package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import com.couchbase.client.core.env.*;
import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.ScopeExistsException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;

public class CapellaProperty2Test {
  private static final Logger LOGGER = LogManager.getLogger(CapellaProperty2Test.class);
  private static final String propertyFile = "test.2.properties";
  public static Properties properties;
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

  @BeforeAll
  public static void setUpBeforeClass() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    properties = new Properties();

    LOGGER.info("Testing with properties file: {}", propertyFile);
    try {
      properties.load(loader.getResourceAsStream(propertyFile));
    } catch (IOException e) {
      LOGGER.debug("can not open properties file: {}", e.getMessage(), e);
    }
  }

  @Test
  public void testCapella1() {
    CouchbaseCapella capella = CouchbaseCapella.getInstance(properties);
    CapellaOrganization organization = CapellaOrganization.getInstance(capella);
    project = CapellaProject.getInstance(organization);
    Assertions.assertNotNull(project.getId());
  }

  @Test
  public void testCapella2() {
    cluster = CapellaCluster.getInstance(project, new CapellaCluster.ClusterConfig().singleNode());
  }

  @Test
  public void testCapella3() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    cidr = CapellaAllowedCIDR.getInstance(cluster);
    cidr.createAllowedCIDR(allowedCIDR);
  }

  @Test
  public void testCapella4() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    user = CapellaCredentials.getInstance(cluster);
    user.createCredential(username, password, new ObjectMapper().createArrayNode());
  }

  @Test
  public void testCapella5() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    bucket = CapellaBucket.getInstance(cluster);
    BucketSettings bucketSettings = BucketSettings.create(bucketName).ramQuotaMB(128).numReplicas(0);
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
    Consumer<IoConfig.Builder> ioConfiguration = ioConfig -> ioConfig
        .numKvConnections(4)
        .networkResolution(NetworkResolution.AUTO)
        .enableMutationTokens(false);
    Consumer<TimeoutConfig.Builder> timeOutConfiguration = timeoutConfig -> timeoutConfig
        .kvTimeout(Duration.ofSeconds(5))
        .connectTimeout(Duration.ofSeconds(15))
        .queryTimeout(Duration.ofSeconds(75));
    ClusterEnvironment environment = ClusterEnvironment
        .builder()
        .securityConfig(secConfiguration)
        .build();
    try (Cluster cluster = Cluster.connect(connectString, ClusterOptions.clusterOptions(authenticator).environment(environment))) {
      cluster.waitUntilReady(Duration.ofSeconds(15));
      Bucket bucket = cluster.bucket(bucketName);
      CollectionManager collectionManager = bucket.collections();
      try {
        RetryLogic.retryVoid(() -> collectionManager.createScope(scopeName));
      } catch (ScopeExistsException e) {
        LOGGER.debug("Scope {} already exists in cluster", scopeName);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      try {
        RetryLogic.retryVoid(() -> collectionManager.createCollection(scopeName, collectionName));
      } catch (CollectionExistsException e) {
        LOGGER.debug("Collection {} already exists in cluster", collectionName);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testCapella7() throws CapellaAPIError {
    Assertions.assertNotNull(bucket);
    bucket.delete();
  }

  @Test
  public void testCapella8() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    cluster.delete();
  }
}
