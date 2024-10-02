package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.logic.OrganizationData;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaOrganization {
  private static final Logger LOGGER = LogManager.getLogger(CapellaOrganization.class);
  private static CapellaOrganization instance;
  private static REST rest;
  public static CouchbaseCapella capella;
  public static String endpoint = "/v4/organizations";
  public static OrganizationData organization;

  private CapellaOrganization() {}

  public static CapellaOrganization getInstance(CouchbaseCapella capella) {
    if (instance == null) {
      instance = new CapellaOrganization();
      instance.attach(capella);
    }
    return instance;
  }

  public void attach(CouchbaseCapella capella) {
    CapellaOrganization.capella = capella;
    CapellaOrganization.rest = CouchbaseCapella.rest;
    organization = getDefaultOrg();
    LOGGER.debug("Organization ID: {}", organization.id);
  }

  public List<OrganizationData> list() throws CapellaAPIError {
    List<OrganizationData> result = new ArrayList<>();
    try {
      ArrayNode reply = rest.get(endpoint).validate().json().get("data").deepCopy();
      reply.forEach(o -> result.add(new OrganizationData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Organization List Error", e);
    }
  }

  public OrganizationData getDefaultOrg() {
    try {
      return list().get(0);
    } catch (IndexOutOfBoundsException | CapellaAPIError e) {
      return new OrganizationData();
    }
  }
}
