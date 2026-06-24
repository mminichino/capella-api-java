package com.codelry.util.capella.logic;

public record CreateCollectionRequest(
    String name,
    int maxTTL
) {}
