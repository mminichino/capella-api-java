package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

public class BucketData {
  public String id;
  public String name;
  public String type;
  public String storageBackend;
  public int memoryAllocationInMb;
  public String bucketConflictResolution;
  public String durabilityLevel;
  public int replicas;
  public boolean flush;
  public boolean flushEnabled;
  public int timeToLiveInSeconds;
  public String evictionPolicy;
  public long itemCount;
  public int opsPerSecond;
  public int diskUsedInMib;
  public int memoryUsedInMib;
  public int priority;

  public BucketData(JsonNode data) {
    id = data.get("id").asText();
    name = data.get("name").asText();
    type = data.get("type").asText();
    storageBackend = data.get("storageBackend").asText();
    memoryAllocationInMb = data.get("memoryAllocationInMb").asInt();
    bucketConflictResolution = data.get("bucketConflictResolution").asText();
    durabilityLevel = data.get("durabilityLevel").asText();
    replicas = data.get("replicas").asInt();
    flush = data.get("flush").asBoolean(false);
    flushEnabled = data.get("flushEnabled").asBoolean(false);
    timeToLiveInSeconds = data.get("timeToLiveInSeconds").asInt();
    evictionPolicy = data.get("evictionPolicy").asText();
    itemCount = data.get("stats").get("itemCount").asLong();
    opsPerSecond = data.get("stats").get("opsPerSecond").asInt();
    diskUsedInMib = data.get("stats").get("diskUsedInMib").asInt();
    memoryUsedInMib = data.get("stats").get("memoryUsedInMib").asInt();
    priority = data.get("priority").asInt();
  }

  public BucketData() {}
}
