package com.codelry.util.capella.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AzureStorageConfig extends StorageConfig {
  private boolean isUltra = false;
  private static final Map<Integer, String> azureStorageMatrix;
  static {
    azureStorageMatrix = new HashMap<>();
    azureStorageMatrix.put(64, "P6");
    azureStorageMatrix.put(128, "P10");
    azureStorageMatrix.put(256, "P15");
    azureStorageMatrix.put(512, "P20");
    azureStorageMatrix.put(1024, "P30");
    azureStorageMatrix.put(2048, "P40");
    azureStorageMatrix.put(4096, "P50");
    azureStorageMatrix.put(8192, "P60");
  }
  private static final Map<Integer, Integer> azureUltraMatrix;
  static {
    azureUltraMatrix = new HashMap<>();
    azureUltraMatrix.put(64, 3000);
    azureUltraMatrix.put(128, 4000);
    azureUltraMatrix.put(256, 6000);
    azureUltraMatrix.put(512, 8000);
    azureUltraMatrix.put(1024, 16000);
    azureUltraMatrix.put(2048, 16000);
    azureUltraMatrix.put(3072, 16000);
    azureUltraMatrix.put(4096, 16000);
    azureUltraMatrix.put(5120, 16000);
    azureUltraMatrix.put(6144, 16000);
    azureUltraMatrix.put(7168, 16000);
    azureUltraMatrix.put(8192, 16000);
  }

  public AzureStorageConfig() {
    configure(256);
  }

  public AzureStorageConfig(int storage) {
    configure(storage);
  }

  public AzureStorageConfig setUltra() {
    this.isUltra = true;
    return this;
  }

  public void configure(int storage) {
    if (isUltra) {
      configureUltra(storage);
    } else {
      configureStandard(storage);
    }
  }

  public void configureUltra(int storage) {
    Optional<Map.Entry<Integer, Integer>> result = azureUltraMatrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isPresent()) {
      Map.Entry<Integer, Integer> entry = result.get();
      this.type = "Ultra";
      this.storage = entry.getKey();
      this.iops = entry.getValue();
    } else {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
  }

  public void configureStandard(int storage) {
    Optional<Map.Entry<Integer, String>> result = azureStorageMatrix.entrySet().stream()
        .filter(entry -> entry.getKey() >= storage)
        .findFirst();
    if (result.isPresent()) {
      Map.Entry<Integer, String> entry = result.get();
      this.type = entry.getValue();
      this.storage = entry.getKey();
    } else {
      throw new IllegalArgumentException("Invalid storage value: " + storage);
    }
  }
}
