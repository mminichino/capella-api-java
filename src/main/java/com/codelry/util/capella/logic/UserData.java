package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class UserData {
  public String id;
  public String name;
  public String email;
  public String status;
  public boolean inactive;
  public String organizationId;
  public List<String> organizationRoles;
  public String lastLogin;
  public String region;
  public String timeZone;
  public boolean enableNotifications;
  public String expiresAt;
  public List<ResourcesData> resources;
  public AuditData audit;

  public UserData(JsonNode data) {
    this.id = data.get("id").asText();
    this.name = data.get("name").asText();
    this.email = data.get("email").asText();
    this.status = data.get("status").asText();
    this.inactive = data.get("inactive").asBoolean();
    this.organizationId = data.get("organizationId").asText();
    this.organizationRoles = new ArrayList<>();
    for (JsonNode role : data.get("organizationRoles")) {
      organizationRoles.add(role.asText());
    }
    this.lastLogin = data.has("lastLogin") ? data.get("lastLogin").asText() : null;
    this.region = data.has("region") ? data.get("region").asText() : null;
    this.timeZone = data.has("timeZone") ? data.get("timeZone").asText() : null;
    this.enableNotifications = data.has("enableNotifications") && data.get("enableNotifications").asBoolean();
    this.expiresAt = data.get("expiresAt").asText();
    this.resources = new ArrayList<>();
    if (data.has("resources")) {
      for (JsonNode resource : data.get("resources")) {
        resources.add(new ResourcesData(resource));
      }
    }
    this.audit = new AuditData(data.get("audit"));
  }

  public UserData() {}
}
