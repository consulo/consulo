package consulo.ide.impl.idea.remoteServer.runtime;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface RemoteOperationCallback {
  void errorOccurred(@Nonnull String errorMessage);
}
