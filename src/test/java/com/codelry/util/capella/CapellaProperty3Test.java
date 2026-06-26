package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.CloudType;
import com.couchbase.client.java.Cluster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@TestMethodOrder(OrderAnnotation.class)
public class CapellaProperty3Test {
  private static final Logger LOGGER = LogManager.getLogger(CapellaProperty3Test.class);
  private static final String propertyFile = "test.3.properties";
  public static Properties properties;
  public String allowedCIDR = "0.0.0.0/0";
  public String database = "testdb";
  public String username = "developer";
  public String password = "#C0uchBas3";
  public static CapellaOrganization organization;
  public static CapellaProject project;
  public static CapellaCluster cluster;
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
  @Order(1)
  public void setProject() {
    CouchbaseCapella capella = CouchbaseCapella.getInstance(properties);
    organization = CapellaOrganization.getInstance(capella);
    project = organization.getDefaultProject();
    Assertions.assertNotNull(project.getId());
  }

  @Test
  @Order(2)
  public void createCluster() {
    cluster = project.createCluster(new CapellaCluster.ClusterConfig().cloudType(CloudType.GCP));
  }

  @Test
  @Order(3)
  public void allowCIDR() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    cidr = cluster.getAllowedCIDR();
    cidr.createAllowedCIDR(allowedCIDR);
    Assertions.assertTrue(new CapellaConnectivity().checkConnectivity(cluster.getConnectString(), Duration.ofMinutes(2)));
  }

  @Test
  @Order(4)
  public void addUser() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    user = cluster.getCredentials();
    user.createCredential(username, password, null);
  }

  @Test
  @Order(5)
  public void runTest() throws CapellaAPIError, NotFoundException {
    CouchbaseCapella capella = CouchbaseCapella.getInstance(properties);
    CapellaOrganization organization = CapellaOrganization.getInstance(capella);
    CapellaProject project = organization.getDefaultProject();
    CapellaCluster cluster = CapellaCluster.getInstance(project, database);
    cluster.getCredentials().addCredentials(username, password);

    Cluster cbCluster = CapellaConnect.connect(cluster);
    try {
      CapellaConnect.clusterWait(cbCluster);
    } catch (Exception e) {
      LOGGER.error("Error connecting to cluster", e);
      Assertions.fail();
    } finally {
      CapellaConnect.disconnect(cbCluster);
    }
  }

  @Test
  @Order(6)
  public void cleanup() throws CapellaAPIError {
    Assertions.assertNotNull(cluster);
    cluster.delete();
  }
}
