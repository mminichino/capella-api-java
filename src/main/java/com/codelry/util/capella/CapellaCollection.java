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
  private static CapellaCollection instance;
  private static REST rest;
  public static String endpoint;
  public static String scopeName;
  public static CollectionData collection;

  private CapellaCollection() {}

  public static CapellaCollection getInstance(CapellaScope scope) {
    if (instance == null) {
      instance = new CapellaCollection();
      instance.attach(scope);
    }
    return instance;
  }

  public static CapellaCollection getInstance(CapellaScope scope, String scopeName) {
    if (instance == null) {
      instance = new CapellaCollection();
      instance.attach(scope, scopeName);
    }
    return instance;
  }

  public void attach(CapellaScope scope) {
    CapellaCollection.rest = CouchbaseCapella.rest;
    if (scope.scope == null) {
      throw new IllegalStateException("Scope must be loaded before attaching collections");
    }
    CapellaCollection.scopeName = scope.scope.name();
    endpoint = scope.endpoint + "/" + CapellaCollection.scopeName + "/collections";
  }

  public void attach(CapellaScope scope, String scopeName) {
    CapellaCollection.rest = CouchbaseCapella.rest;
    CapellaCollection.scopeName = scopeName;
    endpoint = scope.endpoint + "/" + scopeName + "/collections";
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
