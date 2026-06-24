package com.codelry.util.capella.logic;

import java.util.List;
import java.util.Map;

public record AppServiceData(
    String id,
    String name,
    String description,
    String cloudProvider,
    int nodes,
    ComputeData compute,
    String clusterId,
    String currentState,
    String version,
    String plan,
    AuditData audit
) {}
