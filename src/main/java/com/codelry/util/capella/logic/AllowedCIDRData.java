package com.codelry.util.capella.logic;

public record AllowedCIDRData(
    String id,
    String cidr,
    String comment,
    String expiresAt,
    String status,
    String type,
    AuditData audit
) {}
