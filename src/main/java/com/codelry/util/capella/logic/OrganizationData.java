package com.codelry.util.capella.logic;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrganizationData(
    String id,
    String name,
    String description,
    @JsonProperty("preferences") OrganizationPreferences preferences,
    AuditData audit
) {
  public int sessionDuration() {
    return preferences != null ? preferences.sessionDuration() : 0;
  }
}
