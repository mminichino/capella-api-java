package com.codelry.util.capella;

import com.codelry.util.capella.logic.ResourcesData;
import com.codelry.util.capella.logic.UserData;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.codelry.util.capella.RetryLogic.retryReturn;

import java.util.ArrayList;
import java.util.List;

public class CapellaUser extends CapellaOrganization {
  public String userEndpoint;
  public UserData userRecord;

  public CapellaUser(String project, String profile) {
    super(project, profile);
    this.userEndpoint = this.orgEndpoint + "/" + this.organization.id + "/users";
    try {
      this.userRecord = retryReturn(this::getByEmail);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<UserData> listUsers() {
    List<UserData> result = new ArrayList<>();
    try {
      ArrayNode reply = this.rest.getPaged(userEndpoint,
          "page",
          "totalItems",
          "last",
          "perPage",
          100,
          "data",
          "cursor",
          "pages").validate().jsonList();
      reply.forEach(o -> result.add(new UserData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public UserData getByEmail(String email) {
    for (UserData user : listUsers()) {
      if (user.email.equals(email)) {
        return user;
      }
    }
    throw new RuntimeException("No user for email: " + email);
  }

  public UserData getByEmail() {
    for (UserData user : listUsers()) {
      if (user.email.equals(this.getAccountEmail())) {
        return user;
      }
    }
    throw new RuntimeException("No user for email: " + this.getAccountEmail());
  }

  public List<String> getProjects() {
    List<String> result = new ArrayList<>();
    for (ResourcesData resource : userRecord.resources) {
      if (resource.type.equals("project")) {
        result.add(resource.id);
      }
    }
    return result;
  }

  public void setProjectOwnership(String projectId) {
    String userIdEndpoint = userEndpoint + "/" + userRecord.id;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode parameters = mapper.createObjectNode();
    parameters.put("op", "add");
    parameters.put("path", "/resources/" + projectId);
    ObjectNode value = mapper.createObjectNode();
    value.put("type", "project");
    value.put("id", projectId);
    ArrayNode array = mapper.createArrayNode();
    array.add("projectOwner");
    value.set("roles", array);
    parameters.set("value", value);
    ArrayNode payload = mapper.createArrayNode();
    payload.add(parameters);
    try {
      this.rest.patch(userIdEndpoint, payload).validate().json();
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
