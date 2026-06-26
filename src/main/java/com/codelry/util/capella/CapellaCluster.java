package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.*;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CapellaCluster {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCluster.class);

  private final CapellaProject project;
  private final REST rest;
  private final String endpoint;
  private ClusterData cluster;
  private CapellaBucket bucket;
  private CapellaCredentials credentials;
  private CapellaAllowedCIDR allowedCIDR;
  private CapellaCertificate certificate;
  private CapellaAppService appService;

  public static CapellaCluster getInstance(CapellaProject project) {
    return project.getDefaultCluster();
  }

  public static CapellaCluster getInstance(CapellaProject project, ClusterConfig clusterConfig) {
    return project.createCluster(clusterConfig);
  }

  public static CapellaCluster getInstance(CapellaProject project, String clusterName) {
    return project.addCluster(clusterName);
  }

  public static CapellaCluster getInstance(CapellaProject project, String clusterName, ClusterConfig clusterConfig) {
    return project.createCluster(clusterName, clusterConfig);
  }

  CapellaCluster(CapellaProject project) {
    this.project = project;
    this.rest = CouchbaseCapella.rest;
    this.endpoint = project.getEndpoint() + "/" + project.getId() + "/clusters";
  }

  public CapellaProject getProject() {
    return project;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public ClusterData getClusterData() {
    return cluster;
  }

  void resolveCluster() throws NotFoundException, CapellaAPIError {
    if (CouchbaseCapella.hasDatabaseId()) {
      cluster = getById(CouchbaseCapella.getDatabaseId());
    } else if (CouchbaseCapella.hasDatabaseName()) {
      cluster = getByName(CouchbaseCapella.getDatabaseName());
    }
    attachClusterServices();
  }

  void addCluster(String clusterName) throws NotFoundException, CapellaAPIError {
    cluster = getByName(clusterName);
    attachClusterServices();
    populateCertificate();
    project.registerCluster(this);
  }

  public static class ServiceGroupConfig {
    private int cpu = 4;
    private int ram = 16;
    private int storage = 256;
    private int numOfNodes = 3;
    private List<String> services = new ArrayList<>(Arrays.asList("data", "query", "index", "search"));

    public ServiceGroupConfig cpu(int cpu) {
      this.cpu = cpu;
      return this;
    }

    public ServiceGroupConfig ram(int ram) {
      this.ram = ram;
      return this;
    }

    public ServiceGroupConfig storage(int storage) {
      this.storage = storage;
      return this;
    }

    public ServiceGroupConfig numOfNodes(int numOfNodes) {
      this.numOfNodes = numOfNodes;
      return this;
    }

    public ServiceGroupConfig services(List<String> services) {
      this.services = services;
      return this;
    }

    public ServiceGroupRequest toRequest(CloudType cloudType) {
      return new ServiceGroupRequest(
          numOfNodes,
          services,
          new NodeConfig(new ComputeData(cpu, ram), DiskConfig.forCloud(cloudType, storage)));
    }
  }

  public static class ClusterConfig {
    private String description = "Automation Managed Cluster";
    private CloudType cloudType = CloudType.AWS;
    private String cloudRegion = "";
    private String cidr;
    private String version;
    private AvailabilityType availabilityType = AvailabilityType.MULTI_ZONE;
    private SupportPlanType supportPlan = SupportPlanType.DEVELOPER;
    private TimeZoneType timeZone = TimeZoneType.US_WEST;
    private List<ServiceGroupConfig> serviceGroups = new ArrayList<>();

    public ClusterConfig description(String description) {
      this.description = description;
      return this;
    }

    public ClusterConfig cloudType(CloudType cloudType) {
      this.cloudType = cloudType;
      return this;
    }

    public ClusterConfig cloudRegion(String cloudRegion) {
      this.cloudRegion = cloudRegion;
      return this;
    }

    public ClusterConfig cidr(String cidr) {
      this.cidr = cidr;
      return this;
    }

    public ClusterConfig version(String version) {
      this.version = version;
      return this;
    }

    public ClusterConfig availability(AvailabilityType availability) {
      this.availabilityType = availability;
      return this;
    }

    public ClusterConfig supportPlan(SupportPlanType supportPlan) {
      this.supportPlan = supportPlan;
      return this;
    }

    public ClusterConfig timeZone(TimeZoneType timeZone) {
      this.timeZone = timeZone;
      return this;
    }

    public ClusterConfig serviceGroups(List<ServiceGroupConfig> serviceGroups) {
      this.serviceGroups = serviceGroups;
      return this;
    }

    public ClusterConfig addServiceGroup(ServiceGroupConfig serviceGroup) {
      this.serviceGroups.add(serviceGroup);
      return this;
    }

    public ClusterConfig singleNode() {
      this.availabilityType = AvailabilityType.SINGLE_ZONE;
      addServiceGroup(new ServiceGroupConfig().numOfNodes(1).storage(100));
      return this;
    }

    public ClusterConfig singleNode(List<String> services) {
      this.availabilityType = AvailabilityType.SINGLE_ZONE;
      addServiceGroup(new ServiceGroupConfig().numOfNodes(1).storage(100).services(services));
      return this;
    }

    public CreateClusterRequest create(String clusterName) {
      if (serviceGroups.isEmpty()) {
        addServiceGroup(new ServiceGroupConfig());
      }
      if (cloudRegion.isEmpty()) {
        switch (cloudType) {
          case GCP:
            cloudRegion = "us-east4";
            break;
          case AZURE:
            cloudRegion = "eastus";
            break;
          default:
            cloudRegion = "us-east-2";
            break;
        }
      }
      List<ServiceGroupRequest> groups = new ArrayList<>();
      for (ServiceGroupConfig serviceGroup : serviceGroups) {
        groups.add(serviceGroup.toRequest(cloudType));
      }
      return new CreateClusterRequest(
          clusterName,
          description,
          new CloudProviderData(cloudType.name().toLowerCase(), cloudRegion, cidr),
          new CouchbaseServerData(version),
          groups,
          new AvailabilityData(availabilityType.toString()),
          new SupportData(supportPlan.toString(), timeZone.toString()));
    }
  }

  public State wait(String clusterId, State state, StateWaitOperation operation) throws CapellaAPIError {
    String clusterIdEndpoint = endpoint + "/" + clusterId;
    boolean waitForDestroyed = operation == StateWaitOperation.NOT_EQUALS && state == State.DESTROYING;
    for (int retry = 0; retry < 600; retry++) {
      try {
        JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
        String currentState = reply.get("currentState").asText();
        if (State.FAILED.toString().equals(currentState)) {
          return State.FAILED;
        }
        if (waitForDestroyed) {
          Thread.sleep(Duration.ofSeconds(1).toMillis());
          continue;
        }
        boolean check = operation.evaluate(currentState.equals(state.toString()));
        if (!check) {
          Thread.sleep(Duration.ofSeconds(1).toMillis());
          continue;
        }
        return State.HEALTHY;
      } catch (InterruptedException e) {
        LOGGER.debug(e.getMessage(), e);
        return State.UNKNOWN;
      } catch (NotFoundError e) {
        LOGGER.debug("Cluster not found");
        return State.DESTROYED;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Wait Error", e);
      }
    }
    return State.UNKNOWN;
  }

  public ClusterData isCluster(String name) throws CapellaAPIError {
    List<ClusterData> clusters = list();
    for (ClusterData listedCluster : clusters) {
      if (name.equals(listedCluster.name())) {
        return listedCluster;
      }
    }
    return null;
  }

  public void createCluster(String clusterName, ClusterConfig clusterConfig) throws CapellaAPIError {
    ClusterData check = isCluster(clusterName);
    if (check != null) {
      LOGGER.debug("Cluster {} already exists", clusterName);
      State waitResult = wait(check.id(), State.HEALTHY, StateWaitOperation.EQUALS);
      if (waitResult != State.HEALTHY) {
        LOGGER.debug("Existing Cluster {} reached state {}", check.id(), waitResult);
        throw new RuntimeException("Cluster is not healthy: " + waitResult);
      }
      try {
        cluster = getById(check.id());
      } catch (NotFoundException e) {
        throw new RuntimeException("Cluster lookup failed", e);
      }
      attachClusterServices();
      populateCertificate();
      project.registerCluster(this);
      return;
    }
    CreateClusterRequest parameters = clusterConfig.create(clusterName);
    int maxAttempts = 3;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
        String clusterId = reply.get("id").asText();
        LOGGER.debug("Waiting for cluster {} to be healthy (attempt {})", clusterId, attempt);
        State waitResult = wait(clusterId, State.HEALTHY, StateWaitOperation.EQUALS);
        if (waitResult == State.HEALTHY) {
          try {
            cluster = getById(clusterId);
          } catch (NotFoundException e) {
            throw new RuntimeException("Cluster creation failed", e);
          }
          attachClusterServices();
          populateCertificate();
          project.registerCluster(this);
          return;
        }
        if (waitResult == State.FAILED) {
          LOGGER.debug("Cluster {} deployment failed (attempt {})", clusterId, attempt);
          deleteClusterById(clusterId);
          if (attempt < maxAttempts) {
            continue;
          }
          throw new RuntimeException("Cluster creation failed after " + maxAttempts + " attempts");
        }
        LOGGER.debug("Cluster {} reached state {} (attempt {})", clusterId, waitResult, attempt);
        throw new RuntimeException("Cluster creation failed: " + waitResult);
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Cluster Create Error", e);
      }
    }
  }

  private void deleteClusterById(String clusterId) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + clusterId).validate();
      LOGGER.debug("Waiting for cluster {} to be deleted", clusterId);
      State waitResult = wait(clusterId, State.DESTROYING, StateWaitOperation.NOT_EQUALS);
      if (waitResult != State.DESTROYED) {
        LOGGER.debug("Cluster {} deletion reached state {}", clusterId, waitResult);
      }
    } catch (NotFoundError e) {
      LOGGER.debug("Cluster {} already deleted", clusterId);
    } catch (HttpResponseException e) {
      LOGGER.debug("Failed to delete cluster {}: {}", clusterId, e.getMessage());
    }
  }

  private void attachClusterServices() {
    bucket = new CapellaBucket(this);
    credentials = new CapellaCredentials(this);
    allowedCIDR = new CapellaAllowedCIDR(this);
    certificate = new CapellaCertificate(this);
    appService = new CapellaAppService(this);
  }

  private void populateCertificate() {
    try {
      certificate.setCertificate(certificate.getClusterCertificate());
    } catch (CapellaAPIError e) {
      LOGGER.debug("Unable to fetch cluster certificate: {}", e.getMessage());
    }
  }

  public CapellaBucket getBucket() {
    return bucket;
  }

  public CapellaCredentials getCredentials() {
    return credentials;
  }

  public CapellaAllowedCIDR getAllowedCIDR() {
    return allowedCIDR;
  }

  public CapellaCertificate getCertificate() {
    return certificate;
  }

  public CapellaAppService getAppService() {
    return appService;
  }

  public void delete() throws CapellaAPIError {
    if (cluster != null) {
      try {
        String clusterId = cluster.id();
        String clusterName = cluster.name();
        rest.delete(endpoint + "/" + clusterId).validate();
        LOGGER.debug("Waiting for cluster {} to be deleted", clusterName);
        State waitResult = wait(clusterId, State.DESTROYING, StateWaitOperation.NOT_EQUALS);
        if (waitResult != State.DESTROYED) {
          LOGGER.debug("Cluster {} deletion reached state {}", clusterName, waitResult);
          throw new RuntimeException("Cluster deletion failed: " + waitResult);
        }
        cluster = null;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Delete Error", e);
      }
    }
  }

  public List<ClusterData> list() throws CapellaAPIError {
    List<ClusterData> result = new ArrayList<>();
    try {
      ArrayNode reply = rest.getPaged(endpoint,
          "page",
          "totalItems",
          "last",
          "perPage",
          50,
          "data",
          "cursor",
          "pages").validate().jsonList();
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, ClusterData.class)));
      return result;
    } catch (NotFoundError e) {
      LOGGER.debug("Project does not have any clusters");
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster List Error", e);
    }
  }

  public ClusterData getByName(String clusterName) throws NotFoundException, CapellaAPIError {
    if (cluster != null && clusterName.equals(cluster.name())) {
      return cluster;
    }
    CapellaCluster cached = project.findClusterByName(clusterName);
    if (cached != null && cached.cluster != null && clusterName.equals(cached.cluster.name())) {
      return cached.cluster;
    }
    for (ClusterData listedCluster : list()) {
      if (clusterName.equals(listedCluster.name())) {
        return listedCluster;
      }
    }
    throw new NotFoundException("Can not find cluster " + clusterName);
  }

  public ClusterData getById(String id) throws NotFoundException, CapellaAPIError {
    if (cluster != null && cluster.id().equals(id)) {
      return cluster;
    }
    CapellaCluster cached = project.findClusterById(id);
    if (cached != null && cached.cluster != null && cached.cluster.id().equals(id)) {
      return cached.cluster;
    }
    String clusterIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, ClusterData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("Cluster ID not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Get Error", e);
    }
  }

  public void getCluster(String clusterName) throws NotFoundException, CapellaAPIError {
    cluster = getByName(clusterName);
    project.registerCluster(this);
  }

  public void getCluster() throws NotFoundException, CapellaAPIError {
    if (CouchbaseCapella.hasDatabaseId()) {
      cluster = getById(CouchbaseCapella.getDatabaseId());
    } else if (CouchbaseCapella.hasDatabaseName()) {
      cluster = getByName(CouchbaseCapella.getDatabaseName());
    }
    project.registerCluster(this);
  }

  public String getConnectString() {
    if (cluster != null) {
      try {
        cluster = getById(cluster.id());
      } catch (NotFoundException | CapellaAPIError e) {
        LOGGER.debug("Unable to refresh cluster connection string: {}", e.getMessage());
      }
      return cluster.connectionString();
    }
    return null;
  }
}
