package com.codelry.util.capella.logic;

public record AuditData(
    String createdBy,
    String createdAt,
    String modifiedBy,
    String modifiedAt,
    Integer version
) {}
