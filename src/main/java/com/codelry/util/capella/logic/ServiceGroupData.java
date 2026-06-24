package com.codelry.util.capella.logic;

import java.util.List;

public record ServiceGroupData(
    int numOfNodes,
    List<String> services,
    NodeConfig node
) {
  public int cpu() {
    return node != null && node.compute() != null ? node.compute().cpu() : 0;
  }

  public int ram() {
    return node != null && node.compute() != null ? node.compute().ram() : 0;
  }

  public int storage() {
    return node != null && node.disk() != null && node.disk().storage() != null ? node.disk().storage() : 0;
  }

  public String diskType() {
    return node != null && node.disk() != null ? node.disk().type() : null;
  }

  public int iops() {
    return node != null && node.disk() != null && node.disk().iops() != null ? node.disk().iops() : 0;
  }
}
