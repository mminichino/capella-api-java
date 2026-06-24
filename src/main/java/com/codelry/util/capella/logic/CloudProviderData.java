package com.codelry.util.capella.logic;

public record CloudProviderData(
    String type,
    String region,
    String cidr
) {}
