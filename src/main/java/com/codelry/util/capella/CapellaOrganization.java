package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.OrganizationData;
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

public class CapellaOrganization {
  private static final Logger LOGGER = LogManager.getLogger(CapellaOrganization.class);
  public static final String ENDPOINT = "/v4/organizations";

  private final CouchbaseCapella capella;
  private final REST rest;
  private OrganizationData organization;
  private CapellaUser user;
  private final Map<String, CapellaProject> projectsById = new HashMap<>();
  private final Map<String, CapellaProject> projectsByName = new HashMap<>();

  public static CapellaOrganization getInstance(CouchbaseCapella capella) {
    return new CapellaOrganization(capella);
  }

  private CapellaOrganization(CouchbaseCapella capella) {
    this.capella = capella;
    this.rest = CouchbaseCapella.rest;
    this.organization = getDefaultOrg();
    LOGGER.debug("Organization ID: {}", organization.id());
  }

  public CouchbaseCapella getCapella() {
    return capella;
  }

  public REST getRest() {
    return rest;
  }

  public String getEndpoint() {
    return ENDPOINT;
  }

  public OrganizationData getOrganization() {
    return organization;
  }

  public CapellaUser getUser() throws com.codelry.util.capella.exceptions.UserNotConfiguredException {
    if (user == null) {
      user = new CapellaUser(this);
    }
    return user;
  }

  public CapellaProject getDefaultProject() {
    try {
      return getProject(CouchbaseCapella.projectName);
    } catch (CapellaAPIError | NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public CapellaProject getProject(String projectName) throws CapellaAPIError, NotFoundException {
    CapellaProject cached = projectsByName.get(projectName);
    if (cached != null) {
      return cached;
    }
    CapellaProject project = new CapellaProject(this, projectName);
    project.resolveProject();
    registerProject(project);
    return project;
  }

  public CapellaProject getProjectById(String projectId) throws CapellaAPIError, NotFoundException {
    CapellaProject cached = projectsById.get(projectId);
    if (cached != null) {
      return cached;
    }
    ProjectData projectData = fetchProject(projectId);
    CapellaProject project = new CapellaProject(this, projectData);
    registerProject(project);
    return project;
  }

  ProjectData fetchProject(String id) throws CapellaAPIError, NotFoundException {
    String projectIdEndpoint = getEndpoint() + "/" + organization.id() + "/projects/" + id;
    try {
      JsonNode reply = rest.get(projectIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, ProjectData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("Project ID not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Project Get Error", e);
    }
  }

  void registerProject(CapellaProject project) {
    if (project.getProjectData() != null) {
      projectsById.put(project.getId(), project);
      projectsByName.put(project.getProjectName(), project);
    }
  }

  CapellaProject findProjectById(String id) {
    return projectsById.get(id);
  }

  CapellaProject findProjectByName(String name) {
    return projectsByName.get(name);
  }

  public List<OrganizationData> list() throws CapellaAPIError {
    List<OrganizationData> result = new ArrayList<>();
    try {
      ArrayNode reply = rest.get(ENDPOINT).validate().json().get("data").deepCopy();
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, OrganizationData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Organization List Error", e);
    }
  }

  public OrganizationData getById(String id) throws CapellaAPIError, NotFoundException {
    if (organization != null && organization.id().equals(id)) {
      return organization;
    }
    String orgIdEndpoint = ENDPOINT + "/" + id;
    try {
      JsonNode reply = rest.get(orgIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, OrganizationData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("Organization ID not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Organization Get Error", e);
    }
  }

  public OrganizationData getByName(String organizationName) throws NotFoundException, CapellaAPIError {
    if (organization != null && organizationName.equals(organization.name())) {
      return organization;
    }
    for (OrganizationData org : list()) {
      if (organizationName.equals(org.name())) {
        return org;
      }
    }
    throw new NotFoundException("Can not find organization " + organizationName);
  }

  public OrganizationData getDefaultOrg() {
    try {
      if (CouchbaseCapella.hasOrganizationId()) {
        return getById(CouchbaseCapella.getOrganizationId());
      } else if (CouchbaseCapella.hasOrganizationName()) {
        return getByName(CouchbaseCapella.getOrganizationName());
      } else {
        return list().get(0);
      }
    } catch (NotFoundException | IndexOutOfBoundsException | CapellaAPIError e) {
      throw new RuntimeException("Can not find the Capella Organization");
    }
  }
}
