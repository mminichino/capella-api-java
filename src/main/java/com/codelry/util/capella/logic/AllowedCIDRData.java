package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class AllowedCIDRData {
  public String id;
  public String cidr;
  public String comment;
  public String expiresAt;
  public String status;
  public String type;
  public AuditData audit;

  public AllowedCIDRData(JsonNode data) {
    id = data.get("id").asText();
    cidr = data.get("cidr").asText();
    comment = data.get("comment").asText();
    expiresAt = data.has("expiresAt") ? data.get("expiresAt").asText() : null;
    status = data.get("status").asText();
    type = data.get("type").asText();
    if (data.has("audit")) {
      audit = new AuditData(data.get("audit"));
    }
  }

  public AllowedCIDRData() {}
}
