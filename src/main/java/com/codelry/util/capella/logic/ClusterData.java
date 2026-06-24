package com.codelry.util.capella.logic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClusterData(
    String id,
    String appServiceId,
    String name,
    String description,
    String configurationType,
    String connectionString,
    CloudProviderData cloudProvider,
    @JsonProperty("couchbaseServer") CouchbaseServerData couchbaseServer,
    List<ServiceGroupData> serviceGroups,
    AvailabilityData availability,
    SupportData support,
    String currentState,
    AuditData audit,
    String cmekId,
    Boolean enablePrivateDNSResolution
) {
  public String version() {
    return couchbaseServer != null ? couchbaseServer.version() : null;
  }

  public String availabilityType() {
    return availability != null ? availability.type() : null;
  }
}
