package com.codelry.util.capella;

import com.codelry.util.capella.logic.AWSStorageConfig;
import com.codelry.util.capella.logic.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CapellaTest {
  public String profileName = "pytest";
  public String projectName = "pytest-project";
  public String clusterName = "pytest-cluster";
  CapellaProject project;

  @Test
  public void testCapella1() {
    CouchbaseCapella capella = CouchbaseCapella.getInstance(projectName, profileName);
    CapellaOrganization organization = CapellaOrganization.getInstance(capella);
    project = CapellaProject.getInstance(organization);
    Assertions.assertNotNull(project.getId());
  }

  @Test
  public void testCapella2() {
    CapellaCluster cluster = CapellaCluster.getInstance(project);
    CapellaCluster.ClusterBuilder clusterBuilder = new CapellaCluster.ClusterBuilder();
    clusterBuilder.clusterName(clusterName);
    cluster.createCluster(clusterBuilder);
  }
}
