package com.codelry.util.capella.logic;

public enum State {
  HEALTHY("healthy"),
  DEPLOYING("deploying"),
  DESTROYING("destroying"),
  DESTROYED("destroyed"),
  FAILED("deploymentFailed"),
  UNKNOWN("unknown");

  private final String state;

  State(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return state;
  }
}
