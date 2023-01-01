package consulo.util.socketConnection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConnectionState {
  private final String message;
  private final ConnectionStatus status;
  private final Object messageLinkListener;

  public ConnectionState(@Nonnull ConnectionStatus status, @Nullable String message, @Nullable Object messageLinkListener) {
    this.status = status;
    this.message = message;
    this.messageLinkListener = messageLinkListener;
  }

  public ConnectionState(@Nonnull ConnectionStatus status) {
    this(status, null, null);
  }

  @Nonnull
  public ConnectionStatus getStatus() {
    return status;
  }

  @Nonnull
  public String getMessage() {
    return message == null ? status.getStatusText() : message;
  }

  @Nullable
  public Object getMessageLinkListener() {
    return messageLinkListener;
  }
}