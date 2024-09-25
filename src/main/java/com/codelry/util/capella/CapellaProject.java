package com.codelry.util.capella;

import com.codelry.util.capella.logic.ProjectData;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CapellaProject extends CapellaOrganization {
  public String projEndpoint;
  public ProjectData projectRecord;

  public CapellaProject(String project, String profile) {
    super(project, profile);
    this.projEndpoint = this.orgEndpoint + "/" + this.organization.id + "/projects";
  }

  public List<ProjectData> listProjects() {
    List<ProjectData> result = new ArrayList<>();
    try {
      ArrayNode reply = this.rest.getPaged(projEndpoint,
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
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public ProjectData getProject(String id) {
    String projectIdEndpoint = projEndpoint + "/" + id;
    try {
      JsonNode reply = this.rest.get(projectIdEndpoint).validate().json();
      return new ProjectData(reply);
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public ProjectData createProject() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode parameters = mapper.createObjectNode();
    parameters.put("name", this.project);
    parameters.put("description", "Automatically Created Project");
    try {
      JsonNode reply = this.rest.post(projEndpoint, parameters).validate().json();
      String projectId = reply.get("id").asText();
      parameters.put("id", projectId);
      CapellaUser user = new CapellaUser(this.profile, this.project);
      user.setProjectOwnership(projectId);
      return new ProjectData(parameters);
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public CapellaProject getProject() {
    List<ProjectData> projects = getByEmail();
    if (!projects.isEmpty()) {
      for (ProjectData project : projects) {
        if (Objects.equals(project.name, this.project)) {
          projectRecord = project;
          return this;
        }
      }
    }
    projectRecord = createProject();
    return this;
  }

  public List<ProjectData> getByEmail() {
    CapellaUser user = new CapellaUser(this.profile, this.project);
    List<String> projectIds = user.getProjects();
    List<ProjectData> result = new ArrayList<>();
    for (String projectId : projectIds) {
      result.add(getProject(projectId));
    }
    return result;
  }
}
