package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.ProjectData;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaProject {
  private static final Logger LOGGER = LogManager.getLogger(CapellaProject.class);
  private static CapellaProject instance;
  private static REST rest;
  private static CapellaOrganization organization;
  private static CapellaUser user;
  private static String projectName;
  public static String endpoint;
  public static ProjectData project;

  private CapellaProject() {}

  public static CapellaProject getInstance(CapellaOrganization organization) {
    if (instance == null) {
      instance = new CapellaProject();
      instance.attach(organization);
    }
    return instance;
  }

  public void attach(CapellaOrganization organization) {
    CapellaProject.organization = organization;
    CapellaProject.rest = CouchbaseCapella.rest;
    CapellaProject.user = CapellaUser.getInstance(organization);
    endpoint = CapellaOrganization.endpoint + "/" + CapellaOrganization.organization.id + "/projects";
    projectName = CouchbaseCapella.projectName;
    try {
      getProject();
    } catch (CapellaAPIError | NotFoundException e) {
      throw new RuntimeException(e);
    }
    LOGGER.debug("Project ID: {}", project.id);
  }

  public String getProjectName() {
    return projectName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getId() {
    return project.id;
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
      reply.forEach(o -> result.add(new ProjectData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Project List Error", e);
    }
  }

  public ProjectData getProject(String id) throws CapellaAPIError, NotFoundException {
    String projectIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(projectIdEndpoint).validate().json();
      return new ProjectData(reply);
    } catch (NotFoundError e) {
      throw new NotFoundException("Project ID not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Project Get Error", e);
    }
  }

  public ProjectData createProject() throws CapellaAPIError {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode parameters = mapper.createObjectNode();
    parameters.put("name", projectName);
    parameters.put("description", "Automatically Created Project");
    try {
      JsonNode reply = rest.post(endpoint, parameters).validate().json();
      String projectId = reply.get("id").asText();
      parameters.put("id", projectId);
      user.setProjectOwnership(projectId);
      return new ProjectData(parameters);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, parameters, "Project Create Error", e);
    }
  }

  public void getProject() throws CapellaAPIError, NotFoundException {
    if (CouchbaseCapella.hasProjectId()) {
      project = getProject(CouchbaseCapella.getProjectId());
    } else {
      List<ProjectData> projects = getByEmail();
      if (!projects.isEmpty()) {
        for (ProjectData pd : projects) {
          if (projectName.equals(pd.name)) {
            project = pd;
            return;
          }
        }
      }
      project = createProject();
    }
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
