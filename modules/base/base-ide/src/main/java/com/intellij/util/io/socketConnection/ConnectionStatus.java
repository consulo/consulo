package com.intellij.util.io.socketConnection;

import javax.annotation.Nonnull;

public enum ConnectionStatus {
  NOT_CONNECTED("Not connected"), WAITING_FOR_CONNECTION("Waiting for connection"), CONNECTED("Connected"), DISCONNECTED("Disconnected"),
  CONNECTION_FAILED("Connection failed");
  private String myStatusText;

  ConnectionStatus(String statusText) {
    myStatusText = statusText;
  }

  @Nonnull
  public String getStatusText() {
    return myStatusText;
  }
}
