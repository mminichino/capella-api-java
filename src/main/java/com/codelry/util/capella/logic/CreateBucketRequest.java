package com.codelry.util.capella.logic;

public record CreateBucketRequest(
    String name,
    String type,
    String storageBackend,
    int memoryAllocationInMb,
    String bucketConflictResolution,
    String durabilityLevel,
    int replicas,
    boolean flush,
    int timeToLiveInSeconds
) {}
