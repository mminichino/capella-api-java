package com.codelry.util.capella.logic;

public record ProjectData(
    String id,
    String description,
    String name,
    AuditData audit
) {}
