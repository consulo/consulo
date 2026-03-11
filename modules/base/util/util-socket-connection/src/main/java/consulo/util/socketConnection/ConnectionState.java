package consulo.util.socketConnection;

import org.jspecify.annotations.Nullable;

public class ConnectionState {
  @Nullable
  private final String message;
  private final ConnectionStatus status;
  @Nullable
  private final Object messageLinkListener;

  public ConnectionState(ConnectionStatus status, @Nullable String message, @Nullable Object messageLinkListener) {
    this.status = status;
    this.message = message;
    this.messageLinkListener = messageLinkListener;
  }

  public ConnectionState(ConnectionStatus status) {
    this(status, null, null);
  }

  public ConnectionStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message == null ? status.getStatusText() : message;
  }

  @Nullable
  public Object getMessageLinkListener() {
    return messageLinkListener;
  }
}