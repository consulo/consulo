package consulo.remoteServer.runtime;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public interface RemoteOperationCallback {
  void errorOccurred(@Nonnull String errorMessage);
}
