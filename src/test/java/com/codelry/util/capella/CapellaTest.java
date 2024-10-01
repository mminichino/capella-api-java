package com.codelry.util.capella;

import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CapellaTest {
  public String profileName = "pytest";
  public String projectName = "pytest-project";
  public String clusterName = "pytest-cluster";
  public String bucketName = "data";
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
}
