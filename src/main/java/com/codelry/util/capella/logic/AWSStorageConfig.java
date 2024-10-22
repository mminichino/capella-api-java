package com.codelry.util.capella.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AWSStorageConfig extends StorageConfig {
  private static final Map<Integer, Integer> awsStorageMatrix;
  static {
    awsStorageMatrix = new LinkedHashMap<>();
    awsStorageMatrix.put(99, 3000);
    awsStorageMatrix.put(199, 4370);
    awsStorageMatrix.put(299, 5740);
    awsStorageMatrix.put(399, 7110);
    awsStorageMatrix.put(499, 8480);
    awsStorageMatrix.put(599, 9850);
    awsStorageMatrix.put(699, 11220);
    awsStorageMatrix.put(799, 12590);
    awsStorageMatrix.put(899, 13960);
    awsStorageMatrix.put(999, 15330);
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
