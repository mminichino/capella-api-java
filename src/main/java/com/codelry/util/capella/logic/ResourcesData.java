package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ResourcesData {
  public String type;
  public String id;
  public List<String> roles;

  public ResourcesData(JsonNode data) {
    this.type = data.get("type").asText();
    this.id = data.get("id").asText();
    this.roles = new ArrayList<>();
    for (JsonNode role : data.get("roles")) {
      this.roles.add(role.asText());
    }
  }
}
