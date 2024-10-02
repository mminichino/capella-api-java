package com.codelry.util.capella.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapellaAPIError extends Exception {
  private static final Logger LOGGER = LogManager.getLogger(CapellaAPIError.class);

  public CapellaAPIError(int code, byte[] body, JsonNode parameters, String message, Throwable cause) {
    super(message, cause);
    LOGGER.debug("Error Code: {} Message: {}\n{}", code, new String(body), parameters.toPrettyString());
  }

  public CapellaAPIError(int code, byte[] body, String message, Throwable cause) {
    super(message, cause);
    LOGGER.debug("Error Code: {} Message: {}", code, new String(body));
  }
}
