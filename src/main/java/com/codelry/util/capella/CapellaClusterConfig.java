package com.codelry.util.capella;

public class CapellaClusterConfig {
  private int kvEndpoints = 8;
  private int kvTimeout = 5;
  private int connectTimeout = 15;
  private int queryTimeout = 75;

  public CapellaClusterConfig kvEndpoints(int kvEndpoints) {
    this.kvEndpoints = kvEndpoints;
    return this;
  }

  public CapellaClusterConfig kvTimeout(int kvTimeout) {
    this.kvTimeout = kvTimeout;
    return this;
  }

  public CapellaClusterConfig connectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public CapellaClusterConfig queryTimeout(int queryTimeout) {
    this.queryTimeout = queryTimeout;
    return this;
  }

  public CapellaClusterConfig build() {
    return this;
  }

  public int getKvEndpoints() {
    return kvEndpoints;
  }

  public int getKvTimeout() {
    return kvTimeout;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public int getQueryTimeout() {
    return queryTimeout;
  }
}
