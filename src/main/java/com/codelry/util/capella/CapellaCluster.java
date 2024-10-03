package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.*;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

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

  public static CapellaCluster getInstance(CapellaProject project, ClusterConfig clusterConfig) {
    if (instance == null) {
      instance = new CapellaCluster();
      instance.attach(project, clusterConfig);
    }
    return instance;
  }

  public static CapellaCluster getInstance(CapellaProject project, String clusterName, ClusterConfig clusterConfig) {
    if (instance == null) {
      instance = new CapellaCluster();
      instance.attach(project, clusterName, clusterConfig);
    }
    return instance;
  }

  public void attach(CapellaProject project) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id + "/clusters";
    try {
      getCluster();
    } catch (NotFoundException | CapellaAPIError e) {
      throw new RuntimeException("Can not find cluster " + (CouchbaseCapella.hasDatabaseId() ? CouchbaseCapella.getDatabaseId() : CouchbaseCapella.getDatabaseName()), e);
    }
  }

  public void attach(CapellaProject project, ClusterConfig clusterConfig) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id + "/clusters";
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
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id + "/clusters";
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

    private StorageConfig getStorageConfig(CloudType cloudType) {
      switch (cloudType) {
        case AWS:
          return new AWSStorageConfig(storage);
        case AZURE:
          return new AzureStorageConfig(storage);
        default:
          return new GCPStorageConfig(storage);
      }
    }

    public JsonNode create(CloudType cloud) {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode node = mapper.createObjectNode();
      node.put("numOfNodes", numOfNodes);
      ArrayNode servicesNode = mapper.createArrayNode();
      for (String service : services) {
        servicesNode.add(service);
      }
      node.set("services", servicesNode);
      ObjectNode nodeObject = mapper.createObjectNode();
      ObjectNode computeObject = mapper.createObjectNode();
      computeObject.put("cpu", cpu);
      computeObject.put("ram", ram);
      nodeObject.set("compute", computeObject);
      nodeObject.set("disk", getStorageConfig(cloud).asJson());
      node.set("node", nodeObject);
      return node;
    }
  }

  public static class ClusterConfig {
    private String description = "Automation Managed Cluster";
    private CloudType cloudType = CloudType.AWS;
    private String cloudRegion = "us-east-1";
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

    public JsonNode create(String clusterName) {
      if (serviceGroups.isEmpty()) {
        addServiceGroup(new ServiceGroupConfig());
      }
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode node = mapper.createObjectNode();

      node.put("name", clusterName);
      node.put("description", description);

      ObjectNode cloudProvider = mapper.createObjectNode();
      cloudProvider.put("type", cloudType.name().toLowerCase());
      cloudProvider.put("region", cloudRegion);
      if (cidr != null) {
        cloudProvider.put("cidr", cidr);
      }
      node.set("cloudProvider", cloudProvider);

      ObjectNode couchbaseServer = mapper.createObjectNode();
      if (version != null) {
        couchbaseServer.put("version", version);
      }
      node.set("couchbaseServer", couchbaseServer);

      ArrayNode serviceGroupsNode = mapper.createArrayNode();
      for (ServiceGroupConfig serviceGroup : serviceGroups) {
        serviceGroupsNode.add(serviceGroup.create(cloudType));
      }
      node.set("serviceGroups", serviceGroupsNode);

      ObjectNode availability = mapper.createObjectNode();
      availability.put("type", availabilityType.toString());
      node.set("availability", availability);

      ObjectNode support = mapper.createObjectNode();
      support.put("plan", supportPlan.toString());
      support.put("timezone", timeZone.toString());
      node.set("support", support);

      return node;
    }
  }

  public boolean wait(String clusterId, State state, StateWaitOperation operation) throws CapellaAPIError {
    String clusterIdEndpoint = endpoint + "/" + clusterId;
    for (int retry = 0; retry < 36; retry++) {
      try {
        JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
        boolean check = operation.evaluate(reply.get("currentState").asText().equals(state.toString()));
        if (!check) {
          Thread.sleep(Duration.ofSeconds(10).toMillis());
          continue;
        }
        return true;
      } catch (InterruptedException e) {
        LOGGER.debug(e.getMessage(), e);
      } catch (NotFoundError e) {
        LOGGER.debug("Cluster not found");
        if (state == State.DESTROYING) {
          return true;
        }
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Wait Error", e);
      }
    }
    return false;
  }

  public ClusterData isCluster(String name) throws CapellaAPIError {
    List<ClusterData> clusters = list();
    for (ClusterData cluster : clusters) {
      if (name.equals(cluster.name)) {
        return cluster;
      }
    }
    return null;
  }

  public void createCluster(String clusterName, ClusterConfig clusterConfig) throws CapellaAPIError {
    ClusterData check = isCluster(clusterName);
    if (check != null) {
      LOGGER.debug("Cluster {} already exists", clusterName);
      cluster = check;
      return;
    }
    JsonNode parameters = clusterConfig.create(clusterName);
    try {
      JsonNode reply = rest.post(endpoint, parameters).validate().json();
      String clusterId = reply.get("id").asText();
      LOGGER.debug("Waiting for cluster {} to be healthy", clusterId);
      wait(clusterId, State.HEALTHY, StateWaitOperation.EQUALS);
      try {
        cluster = getById(clusterId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Cluster creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, parameters, "Cluster Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (cluster != null) {
      try {
        String clusterIdEndpoint = endpoint + "/" + cluster.id;
        rest.delete(clusterIdEndpoint).validate();
        LOGGER.debug("Waiting for cluster {} to be deleted", cluster.name);
        wait(cluster.id, State.DESTROYING, StateWaitOperation.NOT_EQUALS);
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
      reply.forEach(o -> result.add(new ClusterData(o)));
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
      if (clusterName.equals(cluster.name)) {
        return cluster;
      }
    }
    throw new NotFoundException("Can not find cluster " + clusterName);
  }

  public ClusterData getById(String id) throws NotFoundException, CapellaAPIError {
    String clusterIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
      return new ClusterData(reply);
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
      return cluster.connectionString;
    }
    return null;
  }
}
