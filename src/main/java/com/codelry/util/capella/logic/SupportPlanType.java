package com.codelry.util.capella.logic;

public enum SupportPlanType {
  BASIC("basic"),
  DEVELOPER("developer pro"),
  ENTERPRISE("enterprise");

  private final String plan;

  SupportPlanType(String plan) {
    this.plan = plan;
  }

  @Override
  public String toString() {
    return plan;
  }
}
