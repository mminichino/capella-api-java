package com.codelry.util.capella.logic;

import java.util.List;

public record CreateDatabaseCredentialRequest(
    String name,
    String password,
    List<DatabaseAccessEntry> access
) {}
