package com.codelry.util.capella;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared Jackson helpers for Capella REST request and response handling.
 */
public final class CapellaJson {
  private static final ObjectMapper MAPPER = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .defaultPropertyInclusion(JsonInclude.Value.construct(
          JsonInclude.Include.NON_NULL,
          JsonInclude.Include.NON_NULL))
      .build();

  private CapellaJson() {}

  /**
   * Returns the shared {@link ObjectMapper} used by the Capella API clients.
   *
   * @return configured Jackson mapper
   */
  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /**
   * Converts a Java object into a Jackson {@link JsonNode} for REST requests.
   *
   * @param value object to serialize
   * @return JSON tree representation
   */
  public static JsonNode toJson(Object value) {
    return MAPPER.valueToTree(value);
  }

  /**
   * Deserializes a JSON node into the requested type.
   *
   * @param node JSON node
   * @param type target class
   * @param <T> target type
   * @return deserialized value
   */
  public static <T> T fromJson(JsonNode node, Class<T> type) {
    return MAPPER.convertValue(node, type);
  }

  /**
   * Deserializes a JSON node into the requested generic type.
   *
   * @param node JSON node
   * @param type target type reference
   * @param <T> target type
   * @return deserialized value
   */
  public static <T> T fromJson(JsonNode node, TypeReference<T> type) {
    return MAPPER.convertValue(node, type);
  }

  /**
   * Deserializes each element in a JSON array.
   *
   * @param nodes JSON array
   * @param type target class for each element
   * @param <T> target type
   * @return list of deserialized values
   */
  public static <T> List<T> fromJsonList(ArrayNode nodes, Class<T> type) {
    List<T> result = new ArrayList<>();
    nodes.forEach(node -> result.add(fromJson(node, type)));
    return result;
  }

  /**
   * Parses a PEM-encoded X.509 certificate.
   *
   * @param pem PEM certificate text
   * @return parsed certificate
   */
  public static X509Certificate parseCertificate(String pem) {
    try {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(
          new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    } catch (CertificateException e) {
      throw new IllegalArgumentException("Unable to parse X509 certificate", e);
    }
  }

  /**
   * Parses an X.509 certificate from a Capella certificate API response.
   *
   * @param response JSON response containing a {@code certificate} field
   * @return parsed certificate
   */
  public static X509Certificate parseCertificate(JsonNode response) {
    if (response == null || !response.has("certificate")) {
      throw new IllegalArgumentException("Response does not contain a certificate field");
    }
    return parseCertificate(response.get("certificate").asText());
  }
}
