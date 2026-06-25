package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.AllowedCIDRData;
import com.codelry.util.capella.logic.CreateAllowedCidrRequest;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaAllowedCIDR {
  private static final Logger LOGGER = LogManager.getLogger(CapellaAllowedCIDR.class);

  private final CapellaCluster cluster;
  private final REST rest;
  private final String endpoint;
  private AllowedCIDRData cidr;

  public static CapellaAllowedCIDR getInstance(CapellaCluster cluster) {
    return cluster.getAllowedCIDR();
  }

  CapellaAllowedCIDR(CapellaCluster cluster) {
    this.cluster = cluster;
    this.rest = CouchbaseCapella.rest;
    this.endpoint = cluster.getEndpoint() + "/" + cluster.getClusterData().id() + "/allowedcidrs";
  }

  public AllowedCIDRData isCIDR(String network) throws CapellaAPIError {
    for (AllowedCIDRData listedCidr : list()) {
      if (network.equals(listedCidr.cidr())) {
        return listedCidr;
      }
    }
    return null;
  }

  public void createAllowedCIDR(String network) throws CapellaAPIError {
    AllowedCIDRData check = isCIDR(network);
    if (check != null) {
      LOGGER.debug("CIDR {} already allowed", network);
      cidr = check;
      return;
    }
    LOGGER.debug("Allowing access from network {}", network);
    CreateAllowedCidrRequest parameters = new CreateAllowedCidrRequest(network, "Automatically Created Allowed CIDR Block");
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
      String cidrId = reply.get("id").asText();
      try {
        cidr = getById(cidrId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Allowed CIDR creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Allowed CIDR Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (cidr != null) {
      try {
        rest.delete(endpoint + "/" + cidr.id()).validate();
        LOGGER.debug("CIDR {} deleted", cidr.cidr());
        cidr = null;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Allowed CIDR Delete Error", e);
      }
    }
  }

  public List<AllowedCIDRData> list() throws CapellaAPIError {
    List<AllowedCIDRData> result = new ArrayList<>();
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
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, AllowedCIDRData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Allowed CIDR List Error", e);
    }
  }

  public AllowedCIDRData getByName(String network) throws NotFoundException, CapellaAPIError {
    if (cidr != null && network.equals(cidr.cidr())) {
      return cidr;
    }
    for (AllowedCIDRData listedCidr : list()) {
      if (network.equals(listedCidr.cidr())) {
        return listedCidr;
      }
    }
    throw new NotFoundException("Can not find allowed CIDR " + network);
  }

  public AllowedCIDRData getById(String id) throws NotFoundException, CapellaAPIError {
    if (cidr != null && cidr.id().equals(id)) {
      return cidr;
    }
    String cidrIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(cidrIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, AllowedCIDRData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("CIDR not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Allowed CIDR Get Error", e);
    }
  }

  public void getAllowedCIDR(String network) throws NotFoundException, CapellaAPIError {
    cidr = getByName(network);
  }
}
