package com.codelry.util.capella.logic;

public record BucketStatsData(
    long itemCount,
    int opsPerSecond,
    int diskUsedInMib,
    int memoryUsedInMib
) {}
