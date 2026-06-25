package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.logic.CloudType;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
  public static CapellaOrganization organization;
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
    organization = CapellaOrganization.getInstance(capella);
    project = organization.getDefaultProject();
    Assertions.assertNotNull(project.getId());
  }

  @Test
  public void testCapella2() {
    cluster = project.createCluster(clusterName, new CapellaCluster.ClusterConfig().cloudType(CloudType.GCP));
  }

  @Test
  public void testCapella3() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    cidr = cluster.getAllowedCIDR();
    cidr.createAllowedCIDR(allowedCIDR);
    Assertions.assertTrue(new CapellaConnectivity().checkConnectivity(cluster.getConnectString(), Duration.ofMinutes(2)));
  }

  @Test
  public void testCapella4() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    user = cluster.getCredentials();
    user.createCredential(username, password, null);
  }

  @Test
  public void testCapella5() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    bucket = cluster.getBucket();
    BucketSettings bucketSettings = BucketSettings.create(bucketName).ramQuotaMB(128).numReplicas(1);
    bucket.createBucket(bucketSettings);
    scope = CapellaScope.getInstance(bucket);
    scope.createScope(scopeName);
    collection = CapellaCollection.getInstance(scope);
    collection.createCollection(collectionName);
  }

  @Test
  public void testCapella6() {
    Assertions.assertNotNull(cluster);
    Cluster cbCluster = CapellaConnect.connect(cluster);
    try {
      CapellaConnect.collection(cbCluster, bucketName, scopeName, collectionName).upsert("test", JsonObject.create().put("id", "test"));
    } catch (Exception e) {
      LOGGER.error("Error connecting to cluster", e);
      Assertions.fail();
    } finally {
      CapellaConnect.disconnect(cbCluster);
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
