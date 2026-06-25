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

public class CapellaCollection {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCollection.class);

  private final CapellaScope scope;
  private final REST rest;
  private String endpoint;
  private String scopeName;
  private CollectionData collection;

  public static CapellaCollection getInstance(CapellaScope scope) {
    return new CapellaCollection(scope);
  }

  public static CapellaCollection getInstance(CapellaScope scope, String scopeName) {
    return new CapellaCollection(scope, scopeName);
  }

  public CapellaCollection(CapellaScope scope) {
    this.scope = scope;
    this.rest = CouchbaseCapella.rest;
    if (scope.getScopeData() == null) {
      throw new IllegalStateException("Scope must be loaded before attaching collections");
    }
    this.scopeName = scope.getScopeData().name();
    this.endpoint = scope.getEndpoint() + "/" + scopeName + "/collections";
  }

  public CapellaCollection(CapellaScope scope, String scopeName) {
    this.scope = scope;
    this.rest = CouchbaseCapella.rest;
    this.scopeName = scopeName;
    this.endpoint = scope.getEndpoint() + "/" + scopeName + "/collections";
  }

  public CollectionData getCollectionData() {
    return collection;
  }

  public CollectionData createCollection(String collectionName) throws CapellaAPIError {
    return createCollection(collectionName, 0);
  }

  public CollectionData createCollection(String collectionName, int maxTTL) throws CapellaAPIError {
    CreateCollectionRequest request = new CreateCollectionRequest(collectionName, maxTTL);
    try {
      rest.post(endpoint, CapellaJson.toJson(request)).validate();
      collection = getCollection(collectionName);
      return collection;
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Collection Create Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Collection Create Error", e);
    }
  }

  public List<CollectionData> listCollections() throws CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint).validate().json();
      if (reply.has("collections")) {
        List<CollectionData> result = new ArrayList<>();
        reply.get("collections").forEach(o -> result.add(CapellaJson.fromJson(o, CollectionData.class)));
        return result;
      }
      ScopeData scopeData = CapellaJson.fromJson(reply, ScopeData.class);
      return scopeData.collections() != null ? scopeData.collections() : new ArrayList<>();
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Collection List Error", e);
    }
  }

  public CollectionData getCollection(String collectionName) throws NotFoundException, CapellaAPIError {
    if (collection != null && collectionName.equals(collection.name())) {
      return collection;
    }
    try {
      JsonNode reply = rest.get(endpoint + "/" + collectionName).validate().json();
      collection = CapellaJson.fromJson(reply, CollectionData.class);
      return collection;
    } catch (NotFoundError e) {
      throw new NotFoundException("Collection not found: " + scopeName + "." + collectionName);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Collection Get Error", e);
    }
  }

  public void updateCollection(String collectionName, int maxTTL) throws CapellaAPIError {
    UpdateCollectionRequest request = new UpdateCollectionRequest(maxTTL);
    try {
      rest.put(endpoint + "/" + collectionName, CapellaJson.toJson(request)).validate();
      collection = getCollection(collectionName);
    } catch (NotFoundException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Collection Update Error", e);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, CapellaJson.toJson(request), "Collection Update Error", e);
    }
  }

  public void deleteCollection(String collectionName) throws CapellaAPIError {
    try {
      rest.delete(endpoint + "/" + collectionName).validate();
      if (collection != null && collectionName.equals(collection.name())) {
        collection = null;
      }
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Collection Delete Error", e);
    }
  }
}
