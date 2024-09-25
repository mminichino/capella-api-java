package com.codelry.util.capella;

import com.codelry.util.capella.logic.OrganizationData;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaOrganization extends CouchbaseCapella {
  private static final Logger LOGGER = LogManager.getLogger(CapellaOrganization.class);
  public String orgEndpoint = "/v4/organizations";
  public OrganizationData organization;

  public CapellaOrganization(String project, String profile) {
    super(project, profile);
    this.organization = getDefaultOrg();
    LOGGER.debug("Organization ID: {}", organization.id);
  }

  public List<OrganizationData> listOrg() {
    List<OrganizationData> result = new ArrayList<>();
    try {
      ArrayNode reply = this.rest.get(orgEndpoint).validate().json().get("data").deepCopy();
      reply.forEach(o -> result.add(new OrganizationData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public OrganizationData getDefaultOrg() {
    try {
      return listOrg().get(0);
    } catch (IndexOutOfBoundsException e) {
      return new OrganizationData();
    }
  }
}
