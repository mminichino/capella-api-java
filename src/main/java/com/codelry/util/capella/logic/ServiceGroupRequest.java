package com.codelry.util.capella.logic;

import java.util.List;

public record ServiceGroupRequest(
    int numOfNodes,
    List<String> services,
    NodeConfig node
) {}
