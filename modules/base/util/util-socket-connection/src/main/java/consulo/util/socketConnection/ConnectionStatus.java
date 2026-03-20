package consulo.util.socketConnection;

public enum ConnectionStatus {
  NOT_CONNECTED("Not connected"),
  WAITING_FOR_CONNECTION("Waiting for connection"),
  CONNECTED("Connected"),
  DISCONNECTED("Disconnected"),
  CONNECTION_FAILED("Connection failed");
  private String myStatusText;

  ConnectionStatus(String statusText) {
    myStatusText = statusText;
  }

  public String getStatusText() {
    return myStatusText;
  }
}
