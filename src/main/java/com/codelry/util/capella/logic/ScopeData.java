package com.codelry.util.capella.logic;

import java.util.List;

public record ScopeData(
    String name,
    List<CollectionData> collections
) {}
