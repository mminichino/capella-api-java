package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class ProjectData {
  public String id;
  public String description;
  public String name;
  public AuditData audit;

  public ProjectData(JsonNode data) {
    id = data.get("id").asText();
    description = data.get("description").asText();
    name = data.get("name").asText();
    if (data.has("audit")) {
      audit = new AuditData(data.get("audit"));
    }
  }

  public ProjectData() {}
}
