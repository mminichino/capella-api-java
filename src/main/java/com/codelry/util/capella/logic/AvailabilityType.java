package com.codelry.util.capella.logic;

public enum AvailabilityType {
  SINGLE_ZONE("single"),
  MULTI_ZONE("multi");

  private final String availability;

  AvailabilityType(String availability) {
    this.availability = availability;
  }

  @Override
  public String toString() {
    return availability;
  }
}
