package com.codelry.util.capella.logic;

import java.util.List;

public record AppEndpointOidcConfig(
    String issuer,
    Boolean register,
    String clientId,
    String userPrefix,
    String discoveryUrl,
    String usernameClaim,
    String rolesClaim,
    List<String> scope,
    String providerId,
    Boolean isDefault
) {}
