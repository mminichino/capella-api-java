package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.codelry.util.rest.REST;
import com.codelry.util.rest.exceptions.HttpResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.cert.X509Certificate;

public class CapellaCertificate {
  private static final Logger LOGGER = LogManager.getLogger(CapellaCertificate.class);
  private static CapellaCertificate instance;
  private static REST rest;
  public static String endpoint;

  private CapellaCertificate() {}

  public static CapellaCertificate getInstance(CapellaCluster cluster) {
    if (instance == null) {
      instance = new CapellaCertificate();
      instance.attach(cluster);
    }
    return instance;
  }

  public void attach(CapellaCluster cluster) {
    CapellaCertificate.rest = CouchbaseCapella.rest;
    endpoint = CapellaCluster.endpoint + "/" + CapellaCluster.cluster.id() + "/certificates";
  }

  public X509Certificate getClusterCertificate() throws CapellaAPIError {
    try {
      JsonNode reply = rest.get(endpoint).validate().json();
      return CapellaJson.parseCertificate(reply);
    } catch (HttpResponseException e) {
      throw new CapellaAPIError(rest.responseCode, rest.responseBody, "Cluster Certificate Error", e);
    }
  }
}
