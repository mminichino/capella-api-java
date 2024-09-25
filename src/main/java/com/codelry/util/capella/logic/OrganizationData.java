package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class OrganizationData {
  public String id;
  public String name;
  public String description;
  public int sessionDuration;
  public AuditData audit;

  public OrganizationData(JsonNode data) {
    this.id = data.get("id").asText();
    this.name = data.get("name").asText();
    this.description = data.get("description").asText();
    this.sessionDuration = data.get("preferences").get("sessionDuration").asInt();
    this.audit = new AuditData(data.get("audit"));
  }

  public OrganizationData() {}
}
