package com.codelry.util.capella.logic;

import java.util.List;

public record AppEndpointCollectionsResponse(
    List<AppEndpointCollectionData> collections
) {}
