package com.codelry.util.capella;

import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.bucket.ConflictResolutionType;
import com.couchbase.client.java.manager.bucket.StorageBackend;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

public class CapellaBucketTest {
  private static final Logger LOGGER = LogManager.getLogger(CapellaBucketTest.class);
  private static final String propertyFile = "test.2.properties";
  public static Properties properties;

  @BeforeAll
  public static void setUpBeforeClass() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    properties = new Properties();

    LOGGER.info("Testing with properties file: {}", propertyFile);
    try {
      properties.load(loader.getResourceAsStream(propertyFile));
    } catch (IOException e) {
      LOGGER.debug("can not open properties file: {}", e.getMessage(), e);
    }
  }

  @Test
  public void testBucket1() {
    BucketSettings bucketSettings = BucketSettings.create("name")
        .flushEnabled(false)
        .replicaIndexes(true)
        .ramQuotaMB(128)
        .numReplicas(1)
        .bucketType(BucketType.COUCHBASE)
        .storageBackend(StorageBackend.COUCHSTORE)
        .conflictResolutionType(ConflictResolutionType.SEQUENCE_NUMBER);
    System.out.println(bucketSettings.conflictResolutionType().alias());
    System.out.println(bucketSettings.bucketType().alias());
    System.out.println(bucketSettings.storageBackend().alias());
  }
}
