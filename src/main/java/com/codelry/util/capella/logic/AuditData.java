package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class AuditData {
  public String createdBy;
  public String createdAt;
  public String modifiedBy;
  public String modifiedAt;
  public String version;

  public AuditData(JsonNode data) {
    this.createdBy = data.get("createdBy").asText();
    this.createdAt = data.get("createdAt").asText();
    this.modifiedBy = data.get("modifiedBy").asText();
    this.modifiedAt = data.get("modifiedAt").asText();
    this.version = data.get("version").asText();
  }
}
