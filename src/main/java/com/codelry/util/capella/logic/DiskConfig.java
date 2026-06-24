package com.codelry.util.capella.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record DiskConfig(
    String type,
    Integer storage,
    Integer iops,
    Boolean autoExpansion
) {
  public static DiskConfig aws(int storage) {
    Map<Integer, Integer> matrix = new LinkedHashMap<>();
    matrix.put(99, 3000);
    matrix.put(199, 4370);
    matrix.put(299, 5740);
    matrix.put(399, 7110);
    matrix.put(499, 8480);
    matrix.put(599, 9850);
    matrix.put(699, 11220);
    matrix.put(799, 12590);
    matrix.put(899, 13960);
    matrix.put(999, 15330);
    matrix.put(16384, 16000);
    Optional<Map.Entry<Integer, Integer>> result = matrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
    Map.Entry<Integer, Integer> entry = result.get();
    return new DiskConfig("gp3", storage, entry.getValue(), null);
  }

  public static DiskConfig gcp(int storage) {
    return new DiskConfig("pd-ssd", storage, null, null);
  }

  public static DiskConfig azure(int storage) {
    Map<Integer, String> matrix = new LinkedHashMap<>();
    matrix.put(64, "P6");
    matrix.put(128, "P10");
    matrix.put(256, "P15");
    matrix.put(512, "P20");
    matrix.put(1024, "P30");
    matrix.put(2048, "P40");
    matrix.put(4096, "P50");
    matrix.put(8192, "P60");
    Optional<Map.Entry<Integer, String>> result = matrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
    return new DiskConfig(result.get().getValue(), null, null, true);
  }

  public static DiskConfig azureUltra(int storage) {
    Map<Integer, Integer> matrix = new LinkedHashMap<>();
    matrix.put(64, 3000);
    matrix.put(128, 4000);
    matrix.put(256, 6000);
    matrix.put(512, 8000);
    matrix.put(1024, 16000);
    matrix.put(2048, 16000);
    matrix.put(3072, 16000);
    matrix.put(4096, 16000);
    matrix.put(5120, 16000);
    matrix.put(6144, 16000);
    matrix.put(7168, 16000);
    matrix.put(8192, 16000);
    Optional<Map.Entry<Integer, Integer>> result = matrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
    Map.Entry<Integer, Integer> entry = result.get();
    return new DiskConfig("Ultra", entry.getKey(), entry.getValue(), null);
  }

  public static DiskConfig forCloud(CloudType cloudType, int storage) {
    return switch (cloudType) {
      case AWS -> aws(storage);
      case GCP -> gcp(storage);
      case AZURE -> azure(storage);
    };
  }
}
