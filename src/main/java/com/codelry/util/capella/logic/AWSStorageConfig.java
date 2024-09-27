package com.codelry.util.capella.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AWSStorageConfig extends StorageConfig {
  private static final Map<Integer, Integer> awsStorageMatrix;
  static {
    awsStorageMatrix = new HashMap<>();
    awsStorageMatrix.put(99, 3000);
    awsStorageMatrix.put(199, 5000);
    awsStorageMatrix.put(299, 6000);
    awsStorageMatrix.put(399, 8000);
    awsStorageMatrix.put(499, 9000);
    awsStorageMatrix.put(599, 10000);
    awsStorageMatrix.put(699, 12000);
    awsStorageMatrix.put(799, 13000);
    awsStorageMatrix.put(899, 14000);
    awsStorageMatrix.put(999, 16000);
    awsStorageMatrix.put(16384, 16000);
  }

  public AWSStorageConfig() {
    configure(256);
  }

  public AWSStorageConfig(int storage) {
    configure(storage);
  }

  public void configure(int storage) {
    Optional<Map.Entry<Integer, Integer>> result = awsStorageMatrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isPresent()) {
      Map.Entry<Integer, Integer> entry = result.get();
      this.type = "gp3";
      this.iops = entry.getValue();
      this.storage = storage;
    } else {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
  }
}
