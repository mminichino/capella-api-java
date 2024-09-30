package com.codelry.util.capella.logic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ClusterData {
  public String id;
  public String appServiceId;
  public String name;
  public String description;
  public String configurationType;
  public String connectionString;
  public CloudProviderData cloudProvider;
  public String version;
  public List<ServiceGroupData> serviceGroups;
  public String availability;
  public SupportData support;
  public String currentState;
  public AuditData audit;
  public String cmekId;
  public boolean enablePrivateDNSResolution;

  public ClusterData(JsonNode data) {
    this.id = data.get("id").asText();
    this.appServiceId = data.has("appServiceId") ? data.get("appServiceId").asText() : null;
    this.name = data.get("name").asText();
    this.description = data.get("description").asText();
    this.configurationType = data.has("configurationType") ? data.get("configurationType").asText() : null;
    this.connectionString = data.get("connectionString").asText();
    this.cloudProvider = new CloudProviderData();
    this.cloudProvider.type = data.get("cloudProvider").get("type").asText();
    this.cloudProvider.region = data.get("cloudProvider").get("region").asText();
    this.cloudProvider.cidr = data.get("cloudProvider").has("cidr") ? data.get("cloudProvider").get("cidr").asText() : null;
    this.version = data.get("couchbaseServer").get("version").asText();
    this.serviceGroups = new ArrayList<>();
    for (JsonNode serviceGroup : data.get("serviceGroups")) {
      ServiceGroupData serviceGroupData = new ServiceGroupData();
      serviceGroupData.cpu = serviceGroup.get("node").get("compute").get("cpu").asInt();
      serviceGroupData.ram = serviceGroup.get("node").get("compute").get("ram").asInt();
      serviceGroupData.type = serviceGroup.get("node").get("disk").get("type").asText();
      serviceGroupData.storage = serviceGroup.get("node").get("disk").get("storage").asInt();
      serviceGroupData.iops = serviceGroup.get("node").get("disk").has("iops") ? serviceGroup.get("node").get("disk").get("iops").asInt() : 0;
      serviceGroupData.numOfNodes = serviceGroup.get("numOfNodes").asInt();
      serviceGroupData.services = new ArrayList<>();
      for (JsonNode service : serviceGroup.get("services")) {
        serviceGroupData.services.add(service.asText());
      }
      this.serviceGroups.add(serviceGroupData);
    }
    this.availability = data.get("availability").get("type").asText();
    this.support = new SupportData();
    this.support.plan  = data.get("support").get("plan").asText();
    this.support.timezone = data.get("support").get("timezone").asText();
    this.currentState = data.get("currentState").asText();
    if (data.has("audit")) {
      this.audit = new AuditData(data.get("audit"));
    }
    if (data.has("cmekId")) {
      this.cmekId = data.get("cmekId").asText();
    }
    if (data.has("enablePrivateDNSResolution")) {
      this.enablePrivateDNSResolution = data.get("enablePrivateDNSResolution").asBoolean();
    }
  }
}
