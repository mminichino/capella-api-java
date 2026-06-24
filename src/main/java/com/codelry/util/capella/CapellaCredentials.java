package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.*;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaCredentials {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCredentials.class);
  private static CapellaCredentials instance;
  private static REST rest;
  public static String endpoint;
  public static CredentialData user;

  private CapellaCredentials() {}

  public static CapellaCredentials getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaCredentials();
    }
    instance.attach(cluster);
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaCredentials.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id() + "/users";
  }

  public CredentialData isUser(String username) throws CapellaAPIError {
    for (CredentialData user : list()) {
      if (username.equals(user.name())) {
        return user;
      }
    }
    return null;
  }

  public CreateDatabaseCredentialResponse createCredential(String username, String password, List<DatabaseAccessEntry> access) throws CapellaAPIError {
    CredentialData check = isUser(username);
    if (check != null) {
      LOGGER.debug("User {} already exists", username);
      user = check;
      return new CreateDatabaseCredentialResponse(check.id(), null);
    }
    LOGGER.debug("Creating database credential {}", username);
    List<DatabaseAccessEntry> effectiveAccess = access == null || access.isEmpty() ? defaultAccess() : access;
    CreateDatabaseCredentialRequest parameters = new CreateDatabaseCredentialRequest(username, password, effectiveAccess);
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
      CreateDatabaseCredentialResponse response = CapellaJson.fromJson(reply, CreateDatabaseCredentialResponse.class);
      try {
        user = getById(response.id());
      } catch (NotFoundException e) {
        throw new RuntimeException("Database credential creation failed");
      }
      return response;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Credentials Create Error", e);
    }
  }

  public void updateCredential(String userId, UpdateDatabaseCredentialRequest request) throws CapellaAPIError {
    try {
      rest.put(endpoint + "/" + userId, CapellaJson.toJson(request)).validate();
      user = getById(userId);
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Credentials not found", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Credentials Update Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (user != null) {
      try {
        rest.delete(endpoint + "/" + user.id()).validate();
        LOGGER.debug("User {} deleted", user.name());
        user = null;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Credentials Delete Error", e);
      }
    }
  }

  public List<CredentialData> list() throws CapellaAPIError {
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
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, CredentialData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Credentials List Error", e);
    }
  }

  public CredentialData getByName(String username) throws NotFoundException, CapellaAPIError {
    for (CredentialData user : list()) {
      if (username.equals(user.name())) {
        return user;
      }
    }
    throw new NotFoundException("Can not find user " + username);
  }

  public CredentialData getById(String id) throws NotFoundException, CapellaAPIError {
    String userIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(userIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, CredentialData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("Database credential not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Credentials Get Error", e);
    }
  }

  public void getCredential(String username) throws NotFoundException, CapellaAPIError {
    user = getByName(username);
  }

  private static List<DatabaseAccessEntry> defaultAccess() {
    return List.of(new DatabaseAccessEntry(
        List.of("data_reader", "data_writer"),
        new DatabaseResourceData(List.of(new DatabaseResourceBucketData("*", null)))));
  }
}
