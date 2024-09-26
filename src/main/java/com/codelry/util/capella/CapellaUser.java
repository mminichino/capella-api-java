package com.codelry.util.capella;

import com.codelry.util.capella.logic.ResourcesData;
import com.codelry.util.capella.logic.UserData;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.codelry.util.capella.RetryLogic.retryReturn;

import java.util.ArrayList;
import java.util.List;

public class CapellaUser {
  private static final Logger LOGGER = LogManager.getLogger(CapellaUser.class);
  private static CapellaUser instance;
  private static REST rest;
  private static CapellaOrganization organization;
  public static String email;
  public static String endpoint;
  public static UserData user;

  private CapellaUser() {}

  public static synchronized CapellaUser getInstance(CapellaOrganization organization) {
    if (instance == null) {
      instance = new CapellaUser();
      instance.attach(organization);
    }
    return instance;
  }

  public void attach(CapellaOrganization organization) {
    CapellaUser.organization = organization;
    CapellaUser.rest = CouchbaseCapella.rest;
    email = CapellaOrganization.capella.getAccountEmail();
    endpoint = CapellaOrganization.endpoint + "/" + CapellaOrganization.organization.id + "/users";
    try {
      user = retryReturn(this::getByEmail);
      LOGGER.debug("User ID: {}", user.id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<UserData> listUsers() {
    List<UserData> result = new ArrayList<>();
    try {
      ArrayNode reply = rest.getPaged(endpoint,
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
      if (user.email.equals(email)) {
        return user;
      }
    }
    throw new RuntimeException("No user for email: " + email);
  }

  public List<String> getProjects() {
    List<String> result = new ArrayList<>();
    for (ResourcesData resource : user.resources) {
      if (resource.type.equals("project")) {
        result.add(resource.id);
      }
    }
    return result;
  }

  public void setProjectOwnership(String projectId) {
    String userIdEndpoint = endpoint + "/" + user.id;
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
      rest.patch(userIdEndpoint, payload).validate().json();
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
