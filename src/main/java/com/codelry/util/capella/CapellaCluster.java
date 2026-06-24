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
    }
    instance.attach(project);
    return instance;
  }

  public static CapellaCluster getInstance(CapellaProject project, ClusterConfig clusterConfig) {
    if (instance == null) {
      instance = new CapellaCluster();
    }
    instance.attach(project, clusterConfig);
    return instance;
  }

  public static CapellaCluster getInstance(CapellaProject project, String clusterName, ClusterConfig clusterConfig) {
    if (instance == null) {
      instance = new CapellaCluster();
    }
    instance.attach(project, clusterName, clusterConfig);
    return instance;
  }

  public void attach(CapellaProject project) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id() + "/clusters";
    try {
      getCluster();
    } catch (NotFoundException | CapellaAPIError e) {
      throw new RuntimeException("Can not find cluster " + (CouchbaseCapella.hasDatabaseId() ? CouchbaseCapella.getDatabaseId() : CouchbaseCapella.getDatabaseName()), e);
    }
  }

  public void attach(CapellaProject project, ClusterConfig clusterConfig) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id() + "/clusters";
    clusterName = CouchbaseCapella.hasDatabaseName() ? CouchbaseCapella.getDatabaseName() : NameGenerator.getRandomName();
    try {
      createCluster(clusterName, clusterConfig);
    } catch (CapellaAPIError e) {
      throw new RuntimeException("Can not create cluster " + clusterName, e);
    }
  }

  public void attach(CapellaProject project, String clusterName, ClusterConfig clusterConfig) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id() + "/clusters";
    try {
      createCluster(clusterName, clusterConfig);
    } catch (CapellaAPIError e) {
      throw new RuntimeException("Can not create cluster " + clusterName, e);
    }
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
            cloudRegion = "us-central1";
            break;
          case AZURE:
            cloudRegion = "eastus";
            break;
          default:
            cloudRegion = "us-east-1";
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

  public boolean wait(String clusterId, State state, StateWaitOperation operation) throws CapellaAPIError {
    String clusterIdEndpoint = endpoint + "/" + clusterId;
    for (int retry = 0; retry < 600; retry++) {
      try {
        JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
        boolean check = operation.evaluate(reply.get("currentState").asText().equals(state.toString()));
        if (!check) {
          Thread.sleep(Duration.ofSeconds(1).toMillis());
          continue;
        }
        return false;
      } catch (InterruptedException e) {
        LOGGER.debug(e.getMessage(), e);
        return true;
      } catch (NotFoundError e) {
        LOGGER.debug("Cluster not found");
        return state != State.DESTROYING;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Wait Error", e);
      }
    }
    return true;
  }

  public ClusterData isCluster(String name) throws CapellaAPIError {
    List<ClusterData> clusters = list();
    for (ClusterData cluster : clusters) {
      if (name.equals(cluster.name())) {
        return cluster;
      }
    }
    return null;
  }

  public void createCluster(String clusterName, ClusterConfig clusterConfig) throws CapellaAPIError {
    ClusterData check = isCluster(clusterName);
    if (check != null) {
      LOGGER.debug("Cluster {} already exists", clusterName);
      if (wait(check.id(), State.HEALTHY, StateWaitOperation.EQUALS)) {
        LOGGER.debug("Cluster {} is not healthy", check.id());
        throw new RuntimeException("Cluster is not healthy");
      }
      try {
        cluster = getById(check.id());
      } catch (NotFoundException e) {
        throw new RuntimeException("Cluster lookup failed", e);
      }
      return;
    }
    CreateClusterRequest parameters = clusterConfig.create(clusterName);
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
      String clusterId = reply.get("id").asText();
      LOGGER.debug("Waiting for cluster {} to be healthy", clusterId);
      if (wait(clusterId, State.HEALTHY, StateWaitOperation.EQUALS)) {
        LOGGER.debug("Cluster {} is not healthy", clusterId);
        throw new RuntimeException("Cluster creation failed");
      }
      try {
        cluster = getById(clusterId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Cluster creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Cluster Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (cluster != null) {
      try {
        String clusterIdEndpoint = endpoint + "/" + cluster.id();
        rest.delete(clusterIdEndpoint).validate();
        LOGGER.debug("Waiting for cluster {} to be deleted", cluster.name());
        if (wait(cluster.id(), State.DESTROYING, StateWaitOperation.NOT_EQUALS)) {
          LOGGER.debug("Cluster {} is not deleted", cluster.name());
          throw new RuntimeException("Cluster deletion failed");
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
    List<ClusterData> clusters = list();
    for (ClusterData cluster : clusters) {
      if (clusterName.equals(cluster.name())) {
        return cluster;
      }
    }
    throw new NotFoundException("Can not find cluster " + clusterName);
  }

  public ClusterData getById(String id) throws NotFoundException, CapellaAPIError {
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
  }

  public void getCluster() throws NotFoundException, CapellaAPIError {
    if (CouchbaseCapella.hasDatabaseId()) {
      cluster = getById(CouchbaseCapella.getDatabaseId());
    } else if (CouchbaseCapella.hasDatabaseName()) {
      cluster = getByName(CouchbaseCapella.getDatabaseName());
    }
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
