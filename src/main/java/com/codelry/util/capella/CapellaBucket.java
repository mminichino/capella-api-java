package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.*;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.couchbase.client.core.manager.bucket.CoreBucketSettings;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CapellaBucket {
  private static final Logger LOGGER = LogManager.getLogger(CapellaBucket.class);
  private static CapellaBucket instance;
  private static REST rest;
  private static CapellaCluster cluster;
  private static String bucketName;
  public static String endpoint;
  public static BucketData bucket;

  private CapellaBucket() {}

  public static CapellaBucket getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaBucket();
      instance.attach(cluster);
    }
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaBucket.cluster = cluster;
    CapellaBucket.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id + "/buckets";
  }

  public BucketData isBucket(String name) throws CapellaAPIError {
    List<BucketData> buckets = list();
    for (BucketData bucket : buckets) {
      if (name.equals(bucket.name)) {
        return bucket;
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
    LOGGER.debug("Creating bucket with settings {}", bucketSettings.toString());
    ObjectNode parameters = new ObjectMapper().createObjectNode();
    CoreBucketSettings settings = bucketSettings.toCore();
    parameters.put("name", bucketSettings.name());
    parameters.put("type", bucketSettings.bucketType() != null ? bucketSettings.bucketType().name().toLowerCase() : "couchbase");
    parameters.put("storageBackend", bucketSettings.storageBackend() != null ? bucketSettings.storageBackend().toString().toLowerCase() : "couchstore");
    Optional<Long> quota = Optional.ofNullable(settings.ramQuotaMB());
    parameters.put("memoryAllocationInMb", quota.isPresent() ? bucketSettings.ramQuotaMB() : 128);
    parameters.put("bucketConflictResolution", bucketSettings.conflictResolutionType() != null ? bucketSettings.conflictResolutionType().name().toLowerCase() : "seqno");
    parameters.put("durabilityLevel", bucketSettings.minimumDurabilityLevel() != null ? bucketSettings.minimumDurabilityLevel().name().toLowerCase() : "none");
    Optional<Integer> replicas = Optional.ofNullable(settings.numReplicas());
    parameters.put("replicas", replicas.isPresent() ? bucketSettings.numReplicas() : 1);
    Optional<Boolean> flush = Optional.ofNullable(settings.flushEnabled());
    parameters.put("flush", flush.isPresent() && bucketSettings.flushEnabled());
    parameters.put("timeToLiveInSeconds", bucketSettings.maxExpiry() != null ? bucketSettings.maxExpiry().getSeconds() : 0);
    try {
      JsonNode reply = rest.post(endpoint, parameters).validate().json();
      String bucketId = reply.get("id").asText();
      try {
        bucket = getById(bucketId);
      } catch (NotFoundException e) {
        throw new RuntimeException("Bucket creation failed");
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, parameters, "Bucket Create Error", e);
    }
  }

  public void delete() throws CapellaAPIError {
    if (bucket != null) {
      try {
        String bucketIdEndpoint = endpoint + "/" + bucket.id;
        rest.delete(bucketIdEndpoint).validate();
        LOGGER.debug("Bucket {} deleted", bucket.name);
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
      reply.forEach(o -> result.add(new BucketData(o)));
      return result;
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Bucket List Error", e);
    }
  }

  public BucketData getByName(String bucketName) throws NotFoundException, CapellaAPIError {
    List<BucketData> buckets = list();
    for (BucketData bucket : buckets) {
      if (bucketName.equals(bucket.name)) {
        return bucket;
      }
    }
    throw new NotFoundException("Can not find bucket " + bucketName);
  }

  public BucketData getById(String id) throws NotFoundException, CapellaAPIError {
    String bucketIdEndpoint = endpoint + "/" + id;
    try {
      JsonNode reply = rest.get(bucketIdEndpoint).validate().json();
      return new BucketData(reply);
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
