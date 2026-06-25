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

  private final CapellaCluster cluster;
  private final REST rest;
  private final String endpoint;
  private X509Certificate certificate;

  public static CapellaCertificate getInstance(CapellaCluster cluster) {
    return cluster.getCertificate();
  }

  CapellaCertificate(CapellaCluster cluster) {
    this.cluster = cluster;
    this.rest = CouchbaseCapella.rest;
    this.endpoint = cluster.getEndpoint() + "/" + cluster.getClusterData().id() + "/certificates";
  }

  public X509Certificate getCertificate() {
    return certificate;
  }

  void setCertificate(X509Certificate certificate) {
    this.certificate = certificate;
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
