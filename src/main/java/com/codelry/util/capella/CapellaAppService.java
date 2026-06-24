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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class CapellaAppService {
  private static final Logger LOGGER = LogManager.getLogger(CapellaAppService.class);
  private static CapellaAppService instance;
  private static REST rest;
  public static String endpoint;
  public static String organizationEndpoint;
  public static AppServiceData appService;

  private CapellaAppService() {}

  public static CapellaAppService getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaAppService();
      instance.attach(cluster);
    }
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaAppService.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id() + "/appservices";
    organizationEndpoint = CapellaOrganization.endpoint + "/" + CapellaOrganization.organization.id() + "/appservices";
  }

  public AppServiceData create(CreateAppServiceRequest request) throws CapellaAPIError {
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(request)).validate().json();
      appService = getById(reply.get("id").asText());
      return appService;
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Service Create Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Service Create Error", e);
    }
  }

  public List<AppServiceData> listByOrganization(String projectId) throws CapellaAPIError {
    List<AppServiceData> result = new ArrayList<>();
    try {
      String listEndpoint = organizationEndpoint + "?projectId=" + projectId;
      ArrayNode reply = rest.getPaged(listEndpoint,
          "page",
          "totalItems",
          "last",
          "perPage",
          50,
          "data",
          "cursor",
          "pages").validate().jsonList();
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, AppServiceData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service List Error", e);
    }
  }

  public AppServiceData getById(String appServiceId) throws NotFoundException, CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint + "/" + appServiceId).validate().json();
      appService = CapellaJson.fromJson(reply, AppServiceData.class);
      return appService;
    } catch (NotFoundError e) {
      throw new NotFoundException("App Service not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service Get Error", e);
    }
  }

  public AppServiceData getByName(String name) throws NotFoundException, CapellaAPIError {
    for (AppServiceData service : listByOrganization(CapellaProject.project.id())) {
      if (name.equals(service.name())) {
        appService = service;
        return service;
      }
    }
    throw new NotFoundException("App Service not found: " + name);
  }

  public void update(String appServiceId, UpdateAppServiceRequest request) throws CapellaAPIError {
    try {
      rest.put(endpoint + "/" + appServiceId, CapellaJson.toJson(request)).validate();
      appService = getById(appServiceId);
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Service Update Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "App Service Update Error", e);
    }
  }

  public void delete(String appServiceId) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + appServiceId).validate();
      if (appService != null && appServiceId.equals(appService.id())) {
        appService = null;
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service Delete Error", e);
    }
  }

  public void turnOn(String appServiceId) throws CapellaAPIError {
    try {
      rest.post(endpoint + "/" + appServiceId + "/activationState").validate();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service Turn On Error", e);
    }
  }

  public void turnOff(String appServiceId) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + appServiceId + "/activationState").validate();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service Turn Off Error", e);
    }
  }

  public X509Certificate getCertificate(String appServiceId) throws CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint + "/" + appServiceId + "/certificates").validate().json();
      return CapellaJson.parseCertificate(reply);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "App Service Certificate Error", e);
    }
  }
}
