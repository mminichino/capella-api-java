package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class CredentialData {
  public String id;
  public String name;
  public JsonNode access;
  public AuditData audit;

  public CredentialData(JsonNode data) {
    id = data.get("id").asText();
    name = data.get("name").asText();
    access = data.get("access");
    if (data.has("audit")) {
      audit = new AuditData(data.get("audit"));
    }
  }

  public CredentialData() {}
}
