package com.codelry.util.capella.logic;

import java.util.List;

public record AppEndpointCorsConfig(
    List<String> origin,
    List<String> loginOrigin,
    List<String> headers,
    Integer maxAge,
    Boolean disabled
) {}
