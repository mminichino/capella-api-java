package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import com.couchbase.client.core.env.PasswordAuthenticator;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapellaFileTest {
  private static final Logger LOGGER = LogManager.getLogger(CapellaFileTest.class);
  public String profileName = "junit";
  public String projectName = "junit";
  public String clusterName = "junit-cluster";
  public String bucketName = "data";
  public String scopeName = "group";
  public String collectionName = "table";
  public String allowedCIDR = "0.0.0.0/0";
  public String username = "developer";
  public String password = "#C0uchBas3";
  public static CapellaProject project;
  public static CapellaCluster cluster;
  public static CapellaBucket bucket;
  public static CapellaScope scope;
  public static CapellaCollection collection;
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
    cluster = CapellaCluster.getInstance(project, clusterName, new CapellaCluster.ClusterConfig().singleNode());
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
    user.createCredential(username, password, null);
  }

  @Test
  public void testCapella5() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    bucket = CapellaBucket.getInstance(cluster);
    BucketSettings bucketSettings = BucketSettings.create(bucketName).ramQuotaMB(128).numReplicas(0);
    bucket.createBucket(bucketSettings);
    scope = CapellaScope.getInstance(bucket);
    scope.createScope(scopeName);
    collection = CapellaCollection.getInstance(scope);
    collection.createCollection(collectionName);
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
      Bucket bucket = cluster.bucket(bucketName);
      for (int i = 0; i < 3; i++) {
        try {
          bucket.waitUntilReady(Duration.ofSeconds(5));
          break;
        } catch (UnambiguousTimeoutException ignored) {}
      }
      bucket.scope(scopeName).collection(collectionName).upsert("test", JsonObject.create().put("id", "test"));
    } catch (Exception e) {
      LOGGER.error("Error connecting to cluster", e);
      Assertions.fail();
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
