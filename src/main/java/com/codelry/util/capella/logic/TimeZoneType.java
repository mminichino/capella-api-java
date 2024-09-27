package com.codelry.util.capella.logic;

public enum TimeZoneType {
  US_EAST("ET"),
  US_WEST("PT"),
  EUROPE("GMT"),
  ASIA("IST");

  private final String zone;

  TimeZoneType(String zone) {
    this.zone = zone;
  }

  @Override
  public String toString() {
    return zone;
  }
}
