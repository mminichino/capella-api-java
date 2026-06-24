package com.codelry.util.capella.logic;

import java.util.List;

public record UpdateDatabaseCredentialRequest(
    String password,
    List<DatabaseAccessEntry> access
) {}
