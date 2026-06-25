package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.BucketData;
import com.codelry.util.capella.logic.CreateBucketRequest;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.couchbase.client.core.manager.bucket.CoreBucketSettings;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CapellaBucket {
  private static final Logger LOGGER = LogManager.getLogger(CapellaBucket.class);

  private final CapellaCluster cluster;
  private final REST rest;
  private final String endpoint;
  private BucketData bucket;

  public static CapellaBucket getInstance(CapellaCluster cluster) {
    return cluster.getBucket();
  }

  public static CapellaBucket getInstance(CapellaCluster cluster, String bucketName) throws NotFoundException, CapellaAPIError {
    CapellaBucket bucketService = cluster.getBucket();
    bucketService.getBucket(bucketName);
    return bucketService;
  }

  CapellaBucket(CapellaCluster cluster) {
    this.cluster = cluster;
    this.rest = CouchbaseCapella.rest;
    this.endpoint = cluster.getEndpoint() + "/" + cluster.getClusterData().id() + "/buckets";
  }

  public BucketData getBucketData() {
    return bucket;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public BucketData isBucket(String name) throws CapellaAPIError {
    for (BucketData listedBucket : list()) {
      if (name.equals(listedBucket.name())) {
        return listedBucket;
      }
    }
    return null;
  }

  public void createBucket(BucketSettings bucketSettings) throws CapellaAPIError {
    BucketData check = isBucket(bucketSettings.name());
    if (check != null) {
      LOGGER.debug("Bucket {} already exists", bucketSettings.name());
      bucket = check;
      return;
    }
    LOGGER.debug("Creating bucket with settings {}", bucketSettings);
    CoreBucketSettings settings = bucketSettings.toCore();
    Optional<Long> quota = Optional.ofNullable(settings.ramQuotaMB());
    Optional<Integer> replicas = Optional.ofNullable(settings.numReplicas());
    Optional<Boolean> flush = Optional.ofNullable(settings.flushEnabled());
    CreateBucketRequest parameters = new CreateBucketRequest(
        bucketSettings.name(),
        bucketSettings.bucketType() != null ? bucketSettings.bucketType().alias() : "couchbase",
        bucketSettings.storageBackend() != null ? bucketSettings.storageBackend().alias() : "couchstore",
        quota.orElse(128L).intValue(),
        bucketSettings.conflictResolutionType() != null ? bucketSettings.conflictResolutionType().alias() : "seqno",
        bucketSettings.minimumDurabilityLevel() != null ? bucketSettings.minimumDurabilityLevel().name().toLowerCase() : "none",
        replicas.isPresent() ? bucketSettings.numReplicas() : 1,
        flush.isPresent() && bucketSettings.flushEnabled(),
        bucketSettings.maxExpiry() != null ? (int) bucketSettings.maxExpiry().getSeconds() : 0);
    try {
      JsonNode reply = rest.post(endpoint, CapellaJson.toJson(parameters)).validate().json();
      String bucketId = reply.get("id").asText();
      try {
        bucket = getById(bucketId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Bucket creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(parameters), "Bucket Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (bucket != null) {
      try {
        rest.delete(endpoint + "/" + bucket.id()).validate();
        LOGGER.debug("Bucket {} deleted", bucket.name());
        bucket = null;
      } catch (HttpResponseException e) {
        throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Bucket Delete Error", e);
      }
    }
  }

  public List<BucketData> list() throws CapellaAPIError {
    List<BucketData> result = new ArrayList<>();
    try {
      ArrayNode reply = rest.get(endpoint).validate().json().get("data").deepCopy();
      reply.forEach(o -> result.add(CapellaJson.fromJson(o, BucketData.class)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Bucket List Error", e);
    }
  }

  public BucketData getByName(String bucketName) throws NotFoundException, CapellaAPIError {
    if (bucket != null && bucketName.equals(bucket.name())) {
      return bucket;
    }
    for (BucketData listedBucket : list()) {
      if (bucketName.equals(listedBucket.name())) {
        return listedBucket;
      }
    }
    throw new NotFoundException("Can not find bucket " + bucketName);
  }

  public BucketData getById(String id) throws NotFoundException, CapellaAPIError {
    if (bucket != null && bucket.id().equals(id)) {
      return bucket;
    }
    String bucketIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(bucketIdEndpoint).validate().json();
      return CapellaJson.fromJson(reply, BucketData.class);
    } catch (NotFoundError e) {
      throw new NotFoundException("Bucket not found");
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Bucket Get Error", e);
    }
  }

  public void getBucket(String bucketName) throws NotFoundException, CapellaAPIError {
    bucket = getByName(bucketName);
  }
}
