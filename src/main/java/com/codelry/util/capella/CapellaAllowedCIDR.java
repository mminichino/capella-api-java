package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.AllowedCIDRData;
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
import java.util.List;

public class CapellaAllowedCIDR {
  private static final Logger LOGGER = LogManager.getLogger(CapellaAllowedCIDR.class);
  private static CapellaAllowedCIDR instance;
  private static REST rest;
  private static CapellaCluster cluster;
  private static String bucketName;
  public static String endpoint;
  public static AllowedCIDRData cidr;

  private CapellaAllowedCIDR() {}

  public static CapellaAllowedCIDR getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaAllowedCIDR();
      instance.attach(cluster);
    }
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaAllowedCIDR.cluster = cluster;
    CapellaAllowedCIDR.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id + "/allowedcidrs";
  }

  public AllowedCIDRData isCIDR(String network) throws CapellaAPIError {
    List<AllowedCIDRData> cidrs = list();
    for (AllowedCIDRData cidr : cidrs) {
      if (network.equals(cidr.cidr)) {
        return cidr;
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
    ObjectNode parameters = new ObjectMapper().createObjectNode();
    parameters.put("cidr", network);
    parameters.put("comment", "Automatically Created Allowed CIDR Block");
    try {
      JsonNode reply = rest.post(endpoint, parameters).validate().json();
      String cidrId = reply.get("id").asText();
      try {
        cidr = getById(cidrId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Allowed CIDR creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, parameters, "Allowed CIDR Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (cidr != null) {
      try {
        String cidrIdEndpoint = endpoint + "/" + cidr.id;
        rest.delete(cidrIdEndpoint).validate();
        LOGGER.debug("CIDR {} deleted", cidr.cidr);
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
      reply.forEach(o -> result.add(new AllowedCIDRData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Allowed CIDR List Error", e);
    }
  }

  public AllowedCIDRData getByName(String network) throws NotFoundException, CapellaAPIError {
    List<AllowedCIDRData> cidrs = list();
    for (AllowedCIDRData cidr : cidrs) {
      if (network.equals(cidr.cidr)) {
        return cidr;
      }
    }
    throw new NotFoundException("Can not find allowed CIDR " + network);
  }

  public AllowedCIDRData getById(String id) throws NotFoundException, CapellaAPIError {
    String cidrIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(cidrIdEndpoint).validate().json();
      return new AllowedCIDRData(reply);
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
