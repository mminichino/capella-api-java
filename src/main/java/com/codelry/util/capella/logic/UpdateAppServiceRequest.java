package com.codelry.util.capella.logic;

public record UpdateAppServiceRequest(
    int nodes,
    ComputeData compute
) {}
