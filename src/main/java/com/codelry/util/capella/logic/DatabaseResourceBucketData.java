package com.codelry.util.capella.logic;

import java.util.List;

public record DatabaseResourceBucketData(
    String name,
    List<DatabaseResourceScopeData> scopes
) {}
