package com.codelry.util.capella.logic;

import java.util.Map;

public record AppEndpointScopeConfig(
    Map<String, AppEndpointCollectionConfig> collections
) {}
