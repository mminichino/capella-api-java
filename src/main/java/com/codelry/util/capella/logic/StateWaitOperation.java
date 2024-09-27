package com.codelry.util.capella.logic;

public enum StateWaitOperation {
  EQUALS,
  NOT_EQUALS;

  public boolean evaluate(boolean value) {
    if (this == EQUALS) {
      return value;
    }
    return !value;
  }
}
