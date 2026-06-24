package com.codelry.util.capella.logic;

public record BucketData(
    String id,
    String name,
    String type,
    String storageBackend,
    int memoryAllocationInMb,
    String bucketConflictResolution,
    String durabilityLevel,
    int replicas,
    boolean flush,
    boolean flushEnabled,
    int timeToLiveInSeconds,
    String evictionPolicy,
    BucketStatsData stats,
    Integer priority
) {
  public long itemCount() {
    return stats != null ? stats.itemCount() : 0;
  }

  public int opsPerSecond() {
    return stats != null ? stats.opsPerSecond() : 0;
  }

  public int diskUsedInMib() {
    return stats != null ? stats.diskUsedInMib() : 0;
  }

  public int memoryUsedInMib() {
    return stats != null ? stats.memoryUsedInMib() : 0;
  }
}
