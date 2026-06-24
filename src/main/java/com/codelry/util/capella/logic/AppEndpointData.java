package com.codelry.util.capella.logic;

import java.util.List;
import java.util.Map;

public record AppEndpointData(
    String bucket,
    String name,
    String userXattrKey,
    Boolean disablePublicAllDocs,
    Boolean deltaSyncEnabled,
    Map<String, AppEndpointScopeConfig> scopes,
    AppEndpointCorsConfig cors,
    List<AppEndpointOidcConfig> oidc,
    Map<String, Object> requireResync,
    String adminURL,
    String metricsURL,
    String publicURL,
    String currentState,
    String operationalState
) {}
