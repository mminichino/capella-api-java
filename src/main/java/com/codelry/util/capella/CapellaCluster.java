package com.codelry.util.capella;

import com.codelry.util.capella.logic.ClusterData;
import com.codelry.util.rest.REST;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapellaCluster {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCluster.class);
  private static CapellaCluster instance;
  private static REST rest;
  private static CapellaProject project;
  private static String clusterName;
  public static String endpoint;
  public static ClusterData cluster;

  private CapellaCluster() {}

  public static CapellaCluster getInstance(CapellaProject project) {
    if (instance == null) {
      instance = new CapellaCluster();
      instance.attach(project);
    }
    return instance;
  }

  public void attach(CapellaProject project) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id + "/cluster";
  }
}
