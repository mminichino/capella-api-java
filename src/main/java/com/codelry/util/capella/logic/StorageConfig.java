package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class StorageConfig {
  public CloudType cloudType = CloudType.AWS;
  public String type;
  public int storage = 256;
  public int iops = 0;
  public boolean autoExpansion = true;

  public void setCloudType(CloudType cloudType) {
    this.cloudType = cloudType;
  }

  public boolean hasIops() {
    return iops > 0;
  }

  public CloudType getCloudType() {
    return cloudType;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setStorage(int storage) {
    this.storage = storage;
  }

  public int getStorage() {
    return storage;
  }

  public void setIops(int iops) {
    this.iops = iops;
  }

  public int getIops() {
    return iops;
  }

  public void setAutoExpansion(boolean autoExpansion) {
    this.autoExpansion = autoExpansion;
  }

  public boolean hasAutoExpansion() {
    return autoExpansion;
  }

  public abstract void configure(int storage);

  public JsonNode asJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode node = mapper.createObjectNode();
    node.put("type", type);
    node.put("storage", storage);
    if (iops > 0) {
      node.put("iops", iops);
    }
    if (CloudType.AZURE.equals(cloudType)) {
      node.put("autoExpansion", autoExpansion);
    }
    return node;
  }
}
