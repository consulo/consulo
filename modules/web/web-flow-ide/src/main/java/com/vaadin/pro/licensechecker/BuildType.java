package com.vaadin.pro.licensechecker;

public enum BuildType {
  PRODUCTION("production"),
  DEVELOPMENT("development");

  private String key;

  private BuildType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
