package com.codelry.util.capella;

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

  public void attach(CapellaProject project) {
    CapellaCluster.project = project;
    CapellaCluster.rest = CouchbaseCapella.rest;
    endpoint = CapellaProject.endpoint + "/" + CapellaProject.project.id + "/clusters";
  }

  public static class ServiceGroupBuilder {
    private CloudType cloudType = CloudType.AWS;
    private int cpu = 4;
    private int ram = 16;
    private int storage = 256;
    private int numOfNodes = 3;
    private List<String> services = new ArrayList<>(Arrays.asList("data", "query", "index", "search"));

    public ServiceGroupBuilder cpu(int cpu) {
      this.cpu = cpu;
      return this;
    }

    public ServiceGroupBuilder ram(int ram) {
      this.ram = ram;
      return this;
    }

    public ServiceGroupBuilder storage(int storage) {
      this.storage = storage;
      return this;
    }

    public ServiceGroupBuilder numOfNodes(int numOfNodes) {
      this.numOfNodes = numOfNodes;
      return this;
    }

    public ServiceGroupBuilder services(List<String> services) {
      this.services = services;
      return this;
    }

    public ServiceGroupBuilder cloud(CloudType cloud) {
      this.cloudType = cloud;
      return this;
    }

    private StorageConfig getStorageConfig() {
      switch (cloudType) {
        case AWS:
          return new AWSStorageConfig(storage);
        case AZURE:
          return new AzureStorageConfig(storage);
        default:
          return new GCPStorageConfig(storage);
      }
    }

    public JsonNode build() {
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
      nodeObject.set("disk", getStorageConfig().asJson());
      node.set("node", nodeObject);
      return node;
    }
  }

  public static class ClusterBuilder {
    private String clusterName = "cbdb";
    private String description = "Automation Managed Cluster";
    private CloudType cloudType = CloudType.AWS;
    private String cloudRegion = "us-east-1";
    private String cidr;
    private String version;
    private AvailabilityType availabilityType = AvailabilityType.MULTI_ZONE;
    private SupportPlanType supportPlan = SupportPlanType.DEVELOPER;
    private TimeZoneType timeZone = TimeZoneType.US_WEST;
    private List<ServiceGroupBuilder> serviceGroups = new ArrayList<>();

    public ClusterBuilder clusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public ClusterBuilder description(String description) {
      this.description = description;
      return this;
    }

    public ClusterBuilder cloudType(CloudType cloudType) {
      this.cloudType = cloudType;
      return this;
    }

    public ClusterBuilder cloudRegion(String cloudRegion) {
      this.cloudRegion = cloudRegion;
      return this;
    }

    public ClusterBuilder cidr(String cidr) {
      this.cidr = cidr;
      return this;
    }

    public ClusterBuilder version(String version) {
      this.version = version;
      return this;
    }

    public ClusterBuilder availability(AvailabilityType availability) {
      this.availabilityType = availability;
      return this;
    }

    public ClusterBuilder supportPlan(SupportPlanType supportPlan) {
      this.supportPlan = supportPlan;
      return this;
    }

    public ClusterBuilder timeZone(TimeZoneType timeZone) {
      this.timeZone = timeZone;
      return this;
    }

    public ClusterBuilder serviceGroups(List<ServiceGroupBuilder> serviceGroups) {
      this.serviceGroups = serviceGroups;
      return this;
    }

    public ClusterBuilder addServiceGroup(ServiceGroupBuilder serviceGroup) {
      serviceGroup.cloud(cloudType);
      this.serviceGroups.add(serviceGroup);
      return this;
    }

    public JsonNode build() {
      if (serviceGroups.isEmpty()) {
        addServiceGroup(new ServiceGroupBuilder());
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
      for (ServiceGroupBuilder serviceGroup : serviceGroups) {
        serviceGroupsNode.add(serviceGroup.build());
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

  public boolean wait(String clusterId, State state, StateWaitOperation operation) {
    String clusterIdEndpoint = endpoint + "/" + clusterId;
    for (int retry = 0; retry < 30; retry++) {
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
      } catch (HttpResponseException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return false;
  }

  public ClusterData isCluster(String name) {
    List<ClusterData> clusters = list();
    for (ClusterData cluster : clusters) {
      if (name.equals(cluster.name)) {
        return cluster;
      }
    }
    return null;
  }

  public void createCluster(ClusterBuilder clusterBuilder) {
    ClusterData check = isCluster(clusterBuilder.clusterName);
    if (check != null) {
      LOGGER.debug("Cluster {} already exists", clusterBuilder.clusterName);
      cluster = check;
      return;
    }
    JsonNode parameters = clusterBuilder.build();
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
      LOGGER.error("Code: {} Message: {}\n{}", rest.responseCode, new String(rest.responseBody), parameters.toPrettyString());
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void delete() {
    if (cluster != null) {
      try {
        String clusterIdEndpoint = endpoint + "/" + cluster.id;
        rest.delete(clusterIdEndpoint).validate();
        LOGGER.debug("Cluster {} deleted", cluster.name);
        cluster = null;
      } catch (HttpResponseException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  public List<ClusterData> list() {
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
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public ClusterData getByName(String clusterName) throws NotFoundException {
    List<ClusterData> clusters = list();
    for (ClusterData cluster : clusters) {
      if (clusterName.equals(cluster.name)) {
        return cluster;
      }
    }
    throw new NotFoundException("Can not find cluster " + clusterName);
  }

  public ClusterData getById(String id) throws NotFoundException {
    String clusterIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(clusterIdEndpoint).validate().json();
      return new ClusterData(reply);
    } catch (NotFoundError e) {
      throw new NotFoundException("Cluster ID not found");
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void getCluster(String clusterName) throws NotFoundException {
    cluster = getByName(clusterName);
  }
}
