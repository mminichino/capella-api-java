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
  private static CapellaScope instance;
  private static REST rest;
  public static String endpoint;
  public static ScopeData scope;

  private CapellaScope() {}

  public static CapellaScope getInstance(CapellaBucket bucket) {
    if (instance == null) {
      instance = new CapellaScope();
      instance.attach(bucket);
    }
    return instance;
  }

  public static CapellaScope getInstance(CapellaBucket bucket, String bucketId) {
    if (instance == null) {
      instance = new CapellaScope();
      instance.attach(bucket, bucketId);
    }
    return instance;
  }

  public void attach(CapellaBucket bucket) {
    CapellaScope.rest = CouchbaseCapella.rest;
    if (bucket.bucket == null) {
      throw new IllegalStateException("Bucket must be loaded before attaching scopes");
    }
    endpoint = bucket.endpoint + "/" + bucket.bucket.id() + "/scopes";
  }

  public void attach(CapellaBucket bucket, String bucketId) {
    CapellaScope.rest = CouchbaseCapella.rest;
    endpoint = bucket.endpoint + "/" + bucketId + "/scopes";
  }

  @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
  public ScopeData createScope(String scopeName) throws CapellaAPIError {
    CreateScopeRequest request = new CreateScopeRequest(scopeName);
    try {
      rest.post(endpoint, CapellaJson.toJson(request)).validate();
      scope = getScope(scopeName);
      return scope;
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Scope Create Error", e);
    } catch (HttpResponseException e) {
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
