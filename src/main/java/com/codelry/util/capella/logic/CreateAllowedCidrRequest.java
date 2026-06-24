package com.codelry.util.capella.logic;

public record CreateAllowedCidrRequest(
    String cidr,
    String comment
) {}
