package com.codelry.util.capella.logic;

import java.util.List;

public record DatabaseResourceScopeData(
    String name,
    List<String> collections
) {}
