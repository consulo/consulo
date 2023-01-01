package consulo.util.socketConnection;

import java.util.EventListener;

/**
 * @author nik
 */
public interface SocketConnectionListener extends EventListener {
  void statusChanged(ConnectionStatus status);
}
