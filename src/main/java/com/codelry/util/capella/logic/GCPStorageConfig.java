package com.codelry.util.capella.logic;

public class GCPStorageConfig extends StorageConfig {
  public GCPStorageConfig() {
    configure(256);
  }

  public GCPStorageConfig(int storage) {
    configure(storage);
  }

  public void configure(int storage) {
    this.type = "pd-ssd";
    this.storage = storage;
  }
}
