package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.CredentialData;
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
import java.util.Arrays;
import java.util.List;

public class CapellaCredentials {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCredentials.class);
  private static CapellaCredentials instance;
  private static REST rest;
  private static CapellaCluster cluster;
  public static String endpoint;
  public static CredentialData user;

  private CapellaCredentials() {}

  public static CapellaCredentials getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaCredentials();
      instance.attach(cluster);
    }
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaCredentials.cluster = cluster;
    CapellaCredentials.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id + "/users";
  }

  public CredentialData isUser(String username) {
    List<CredentialData> users = list();
    for (CredentialData user : users) {
      if (username.equals(user.name)) {
        return user;
      }
    }
    return null;
  }

  public void createCredential(String username, String password, ArrayNode access) {
    CredentialData check = isUser(username);
    if (check != null) {
      LOGGER.debug("User {} already exists", username);
      user = check;
      return;
    }
    LOGGER.debug("Creating database user {}", username);
    ObjectNode parameters = new ObjectMapper().createObjectNode();
    parameters.put("name", username);
    parameters.put("password", password);
    if (access.isEmpty()) {
      ObjectNode permissions = new ObjectMapper().createObjectNode();
      ArrayNode privileges =  new ObjectMapper().createArrayNode();
      String[] level = {"data_reader", "data_writer"};
      for (String l : level) {
        privileges.add(l);
      }
      permissions.set("privileges", privileges);
      ObjectNode bucket = new ObjectMapper().createObjectNode();
      bucket.put("name", "*");
      ArrayNode buckets = new ObjectMapper().createArrayNode();
      buckets.add(bucket);
      permissions.set("resources", new ObjectMapper().createObjectNode().set("buckets", buckets));
      access.add(permissions);
    }
    parameters.set("access", access);
    try {
      JsonNode reply = rest.post(endpoint, parameters).validate().json();
      String userId = reply.get("id").asText();
      try {
        user = getById(userId);
      } catch (NotFoundException e) {
        throw new RuntimeException("User creation failed");
      }
    } catch (HttpResponseException e) {
      LOGGER.error("Code: {} Message: {}\n{}", rest.responseCode, new String(rest.responseBody), parameters.toPrettyString());
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void delete() {
    if (user != null) {
      try {
        String cidrIdEndpoint = endpoint + "/" + user.id;
        rest.delete(cidrIdEndpoint).validate();
        LOGGER.debug("User {} deleted", user.name);
        user = null;
      } catch (HttpResponseException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  public List<CredentialData> list() {
    List<CredentialData> result = new ArrayList<>();
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
      reply.forEach(o -> result.add(new CredentialData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public CredentialData getByName(String username) throws NotFoundException {
    List<CredentialData> users = list();
    for (CredentialData user : users) {
      if (username.equals(user.name)) {
        return user;
      }
    }
    throw new NotFoundException("Can not find user " + username);
  }

  public CredentialData getById(String id) throws NotFoundException {
    String userIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(userIdEndpoint).validate().json();
      return new CredentialData(reply);
    } catch (NotFoundError e) {
      throw new NotFoundException("CIDR not found");
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void getCredential(String username) throws NotFoundException {
    user = getByName(username);
  }
}
