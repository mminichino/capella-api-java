package com.codelry.util.capella.logic;

import java.util.List;

public record CredentialData(
    String id,
    String name,
    List<DatabaseAccessEntry> access,
    AuditData audit
) {}
