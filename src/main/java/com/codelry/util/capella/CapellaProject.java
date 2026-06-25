package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.exceptions.UserNotConfiguredException;
import com.codelry.util.capella.logic.CreateProjectRequest;
import com.codelry.util.capella.logic.IdResponse;
import com.codelry.util.capella.logic.NameGenerator;
import com.codelry.util.capella.logic.ProjectData;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapellaProject {
  private static final Logger LOGGER = LogManager.getLogger(CapellaProject.class);

  private final CapellaOrganization organization;
  private final REST rest;
  private final String endpoint;
  private final String projectName;
  private ProjectData project;
  private CapellaUser user;
  private final Map<String, CapellaCluster> clustersById = new HashMap<>();
  private final Map<String, CapellaCluster> clustersByName = new HashMap<>();

  public static CapellaProject getInstance(CapellaOrganization organization) {
    return organization.getDefaultProject();
  }

  CapellaProject(CapellaOrganization organization, String projectName) {
    this.organization = organization;
    this.rest = organization.getRest();
    this.endpoint = organization.getEndpoint() + "/" + organization.getOrganization().id() + "/projects";
    this.projectName = projectName;
  }

  CapellaProject(CapellaOrganization organization, ProjectData project) {
    this.organization = organization;
    this.rest = organization.getRest();
    this.endpoint = organization.getEndpoint() + "/" + organization.getOrganization().id() + "/projects";
    this.projectName = project.name();
    this.project = project;
  }

  void resolveProject() throws CapellaAPIError, NotFoundException {
    try {
      user = organization.getUser();
    } catch (UserNotConfiguredException e) {
      throw new RuntimeException("Capella user not configured. Please set capella.user.email or capella.user.id.");
    }
    getProject();
    LOGGER.debug("Project ID: {}", project.id());
  }

  public String getProjectName() {
    return projectName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getId() {
    return project.id();
  }

  public ProjectData getProjectData() {
    return project;
  }

  public CapellaOrganization getOrganization() {
    return organization;
  }

  public CapellaCluster getDefaultCluster() {
    CapellaCluster cluster = new CapellaCluster(this);
    try {
      cluster.resolveCluster();
      if (cluster.getClusterData() != null) {
        registerCluster(cluster);
      }
    } catch (NotFoundException e) {
      LOGGER.debug("Cluster not found: {}", e.getMessage());
    } catch (CapellaAPIError e) {
      throw new RuntimeException("Can not find cluster " + (CouchbaseCapella.hasDatabaseId() ? CouchbaseCapella.getDatabaseId() : CouchbaseCapella.getDatabaseName()), e);
    }
    return cluster;
  }

  public CapellaCluster createCluster(CapellaCluster.ClusterConfig clusterConfig) {
    String clusterName = CouchbaseCapella.hasDatabaseName() ? CouchbaseCapella.getDatabaseName() : NameGenerator.getRandomName();
    return createCluster(clusterName, clusterConfig);
  }

  public CapellaCluster createCluster(String clusterName, CapellaCluster.ClusterConfig clusterConfig) {
    CapellaCluster cached = findClusterByName(clusterName);
    if (cached != null) {
      try {
        cached.createCluster(clusterName, clusterConfig);
      } catch (CapellaAPIError e) {
        throw new RuntimeException("Can not create cluster " + clusterName, e);
      }
      return cached;
    }
    CapellaCluster cluster = new CapellaCluster(this);
    try {
      cluster.createCluster(clusterName, clusterConfig);
    } catch (CapellaAPIError e) {
      throw new RuntimeException("Can not create cluster " + clusterName, e);
    }
    registerCluster(cluster);
    return cluster;
  }

  void registerCluster(CapellaCluster cluster) {
    if (cluster.getClusterData() != null) {
      clustersById.put(cluster.getClusterData().id(), cluster);
      clustersByName.put(cluster.getClusterData().name(), cluster);
    }
  }

  CapellaCluster findClusterById(String id) {
    return clustersById.get(id);
  }

  CapellaCluster findClusterByName(String name) {
    return clustersByName.get(name);
  }

  public List<ProjectData> listProjects() throws CapellaAPIError {
    List<ProjectData> result = new ArrayList<>();
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
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, ProjectData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Project List Error", e);
    }
  }

  public ProjectData getProject(String id) throws CapellaAPIError, NotFoundException {
    if (project != null && project.id().equals(id)) {
      return project;
    }
    CapellaProject cached = organization.findProjectById(id);
    if (cached != null && cached.project != null && cached.project.id().equals(id)) {
      return cached.project;
    }
    return organization.fetchProject(id);
  }

  public ProjectData createProject() throws CapellaAPIError {
    CreateProjectRequest parameters = new CreateProjectRequest(projectName, "Automatically Created Project");
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
      IdResponse idResponse = CapellaJson.fromJson(reply, IdResponse.class);
      user.setProjectOwnership(idResponse.id());
      project = getProject(idResponse.id());
      organization.registerProject(this);
      return project;
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Project Create Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Project Create Error", e);
    }
  }

  public void getProject() throws CapellaAPIError, NotFoundException {
    if (CouchbaseCapella.hasProjectId()) {
      project = getProject(CouchbaseCapella.getProjectId());
    } else {
      List<ProjectData> projects = getByEmail();
      if (!projects.isEmpty()) {
        for (ProjectData pd : projects) {
          if (projectName.equals(pd.name())) {
            project = pd;
            return;
          }
        }
      }
      project = createProject();
    }
  }

  public void getProjectByName(String name) throws CapellaAPIError, NotFoundException {
    if (project != null && name.equals(project.name())) {
      return;
    }
    CapellaProject cached = organization.findProjectByName(name);
    if (cached != null && cached.project != null) {
      project = cached.project;
      return;
    }
    for (ProjectData pd : listProjects()) {
      if (name.equals(pd.name())) {
        project = pd;
        organization.registerProject(this);
        return;
      }
    }
    throw new NotFoundException("Project not found: " + name);
  }

  public List<ProjectData> getByEmail() throws CapellaAPIError, NotFoundException {
    List<String> projectIds = user.getProjects();
    List<ProjectData> result = new ArrayList<>();
    for (String projectId : projectIds) {
      result.add(getProject(projectId));
    }
    return result;
  }
}
