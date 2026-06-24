package com.codelry.util.capella.logic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateClusterRequest(
    String name,
    String description,
    CloudProviderData cloudProvider,
    @JsonProperty("couchbaseServer") CouchbaseServerData couchbaseServer,
    List<ServiceGroupRequest> serviceGroups,
    AvailabilityData availability,
    SupportData support
) {}
