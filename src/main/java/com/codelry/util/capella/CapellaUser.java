package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.exceptions.UserNotConfiguredException;
import com.codelry.util.capella.logic.ResourcesData;
import com.codelry.util.capella.logic.UserData;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.codelry.util.capella.RetryLogic.retryReturn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CapellaUser {
  private static final Logger LOGGER = LogManager.getLogger(CapellaUser.class);
  private static CapellaUser instance;
  private static REST rest;
  private static CapellaOrganization organization;
  public static String email;
  public static String endpoint;
  public static UserData user;

  private CapellaUser() {}

  public static synchronized CapellaUser getInstance(CapellaOrganization organization) throws UserNotConfiguredException {
    if (instance == null) {
      instance = new CapellaUser();
      instance.attach(organization);
    }
    return instance;
  }

  public void attach(CapellaOrganization organization) throws UserNotConfiguredException {
    CapellaUser.organization = organization;
    CapellaUser.rest = CouchbaseCapella.rest;
    endpoint = CapellaOrganization.endpoint + "/" + CapellaOrganization.organization.id + "/users";
    try {
      if (CouchbaseCapella.hasAccountEmail()) {
        email = CouchbaseCapella.getAccountEmail();
        user = retryReturn(this::getByEmail);
      } else if (CouchbaseCapella.hasAccountId()) {
        String accountId = CouchbaseCapella.getAccountId();
        user = getById(accountId);
        email = user.email;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (user == null || email == null) {
      throw new UserNotConfiguredException("Capella user not configured");
    }
    LOGGER.debug("User ID: {} ({})", user.id, user.email);
  }

  public static List<UserData> listUsers() throws CapellaAPIError {
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
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "User List Error", e);
    }
  }

  public static List<UserData> getUniqueUsers() throws CapellaAPIError {
    List<UserData> result = listUsers();
    long size = result.size();
    Set<UserData> userSet = new HashSet<>(result);
    while (userSet.size() < size) {
      List<UserData> update = CapellaUser.listUsers();
      userSet.addAll(update);
    }
    return new ArrayList<>(userSet);
  }

  public UserData getById(String id) throws CapellaAPIError, NotFoundException {
    String userIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(userIdEndpoint).validate().json();
      return new UserData(reply);
    } catch (NotFoundError e) {
      throw new NotFoundException("User ID not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "User Get Error", e);
    }
  }

  public UserData getByEmail(String email) throws CapellaAPIError {
    for (UserData user : listUsers()) {
      if (user.email.equals(email)) {
        return user;
      }
    }
    throw new RuntimeException("No user for email: " + email);
  }

  public UserData getByEmail() throws CapellaAPIError {
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

  public void setProjectOwnership(String projectId) throws CapellaAPIError {
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
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, parameters, "User Set Error", e);
    }
  }
}
