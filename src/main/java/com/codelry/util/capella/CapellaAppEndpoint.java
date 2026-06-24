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

public class CapellaAppEndpoint {
  private static final Logger LOGGER = LogManager.getLogger(CapellaAppEndpoint.class);
  private static CapellaAppEndpoint instance;
  private static REST rest;
  public static String endpoint;
  public static AppEndpointData appEndpoint;

  private CapellaAppEndpoint() {}

  public static CapellaAppEndpoint getInstance(CapellaAppService appService) {
    if (instance == null) {
      instance = new CapellaAppEndpoint();
      instance.attach(appService);
    }
    return instance;
  }

  public void attach(CapellaAppService appService) {
    CapellaAppEndpoint.rest = CouchbaseCapella.rest;
    if (appService.appService == null) {
      throw new IllegalStateException("App Service must be loaded before attaching App Endpoints");
    }
    endpoint = appService.endpoint + "/" + appService.appService.id() + "/appEndpoints";
  }

  public void attach(CapellaAppService appService, String appServiceId) throws NotFoundException, CapellaAPIError {
    CapellaAppEndpoint.rest = CouchbaseCapella.rest;
    appService.getById(appServiceId);
    endpoint = appService.endpoint + "/" + appServiceId + "/appEndpoints";
  }

  public AppEndpointData create(CreateAppEndpointRequest request) throws CapellaAPIError {
    try {
      rest.post(endpoint, CapellaJson.toJson(request)).validate();
      appEndpoint = getByName(request.name());
      return appEndpoint;
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Endpoint Create Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Endpoint Create Error", e);
    }
  }

  public List<AppEndpointData> list() throws CapellaAPIError {
    List<AppEndpointData> result = new ArrayList<>();
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
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, AppEndpointData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint List Error", e);
    }
  }

  public AppEndpointData getByName(String appEndpointName) throws NotFoundException, CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint + "/" + appEndpointName).validate().json();
      appEndpoint = CapellaJson.fromJson(reply, AppEndpointData.class);
      return appEndpoint;
    } catch (NotFoundError e) {
      throw new NotFoundException("App Endpoint not found: " + appEndpointName);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint Get Error", e);
    }
  }

  public void update(String appEndpointName, CreateAppEndpointRequest request) throws CapellaAPIError {
    try {
      rest.put(endpoint + "/" + appEndpointName, CapellaJson.toJson(request)).validate();
      appEndpoint = getByName(appEndpointName);
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Endpoint Update Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Endpoint Update Error", e);
    }
  }

  public void delete(String appEndpointName) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + appEndpointName).validate();
      if (appEndpoint != null && appEndpointName.equals(appEndpoint.name())) {
        appEndpoint = null;
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint Delete Error", e);
    }
  }

  public void resume(String appEndpointName) throws CapellaAPIError {
    try {
      rest.post(endpoint + "/" + appEndpointName + "/activationStatus").validate();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint Resume Error", e);
    }
  }

  public void pause(String appEndpointName) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + appEndpointName + "/activationStatus").validate();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint Pause Error", e);
    }
  }

  public AppEndpointCollectionsResponse listCollections(String appEndpointName) throws CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint + "/" + appEndpointName + "/collections").validate().json();
      return CapellaJson.fromJson(reply, AppEndpointCollectionsResponse.class);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Endpoint Collections List Error", e);
    }
  }
}
