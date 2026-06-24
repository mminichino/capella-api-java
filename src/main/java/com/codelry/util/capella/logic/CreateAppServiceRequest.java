package com.codelry.util.capella.logic;

public record CreateAppServiceRequest(
    String name,
    String description,
    int nodes,
    ComputeData compute,
    String version
) {}
