package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.capella.exceptions.NotFoundException;
import com.codelry.util.capella.logic.*;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.codelry.util.rest.exceptions.NotFoundError;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapellaScope {
  private static final Logger LOGGER = LogManager.getLogger(CapellaScope.class);

  private final CapellaBucket bucket;
  private final REST rest;
  private String endpoint;
  private ScopeData scope;

  public static CapellaScope getInstance(CapellaBucket bucket) {
    return new CapellaScope(bucket);
  }

  public static CapellaScope getInstance(CapellaBucket bucket, String bucketId) {
    return new CapellaScope(bucket, bucketId);
  }

  public CapellaScope(CapellaBucket bucket) {
    this.bucket = bucket;
    this.rest = CouchbaseCapella.rest;
    if (bucket.getBucketData() == null) {
      throw new IllegalStateException("Bucket must be loaded before attaching scopes");
    }
    this.endpoint = bucket.getEndpoint() + "/" + bucket.getBucketData().id() + "/scopes";
  }

  public CapellaScope(CapellaBucket bucket, String bucketId) {
    this.bucket = bucket;
    this.rest = CouchbaseCapella.rest;
    this.endpoint = bucket.getEndpoint() + "/" + bucketId + "/scopes";
  }

  public ScopeData getScopeData() {
    return scope;
  }

  public String getEndpoint() {
    return endpoint;
  }

  @Retryable(backoff = @Backoff(multiplier = 2))
  public ScopeData createScope(String scopeName) throws CapellaAPIError {
    try {
      return RetryExecutor.execute(
          CapellaScope.class.getMethod("createScope", String.class),
          () -> createScopeOnce(scopeName));
    } catch (CapellaAPIError e) {
      throw e;
    } catch (Exception e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Scope Create Error", e);
    }
  }

  private ScopeData createScopeOnce(String scopeName) throws CapellaAPIError {
    CreateScopeRequest request = new CreateScopeRequest(scopeName);
    try {
      rest.post(endpoint, CapellaJson.toJson(request)).validate();
      scope = getScope(scopeName);
      return scope;
    } catch (NotFoundException | HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Scope Create Error", e);
    }
  }

  public List<ScopeData> listScopes() throws CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint).validate().json();
      ScopeListResponse response = CapellaJson.fromJson(reply, ScopeListResponse.class);
      return response.scopes() != null ? response.scopes() : new ArrayList<>();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Scope List Error", e);
    }
  }

  public ScopeData getScope(String scopeName) throws NotFoundException, CapellaAPIError {
    if (scope != null && scopeName.equals(scope.name())) {
      return scope;
    }
    try {
      JsonNode reply = rest.get(endpoint + "/" + scopeName).validate().json();
      scope = CapellaJson.fromJson(reply, ScopeData.class);
      return scope;
    } catch (NotFoundError e) {
      throw new NotFoundException("Scope not found: " + scopeName);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Scope Get Error", e);
    }
  }

  public void deleteScope(String scopeName) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + scopeName).validate();
      if (scope != null && scopeName.equals(scope.name())) {
        scope = null;
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Scope Delete Error", e);
    }
  }
}
