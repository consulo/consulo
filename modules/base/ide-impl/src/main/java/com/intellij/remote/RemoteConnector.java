package com.intellij.remote;

import com.intellij.util.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author traff
 */
public interface RemoteConnector {
  @Nullable
  String getId();

  @Nonnull
  String getName();

  @Nonnull
  RemoteConnectionType getType();

  void produceRemoteCredentials(Consumer<RemoteCredentials> remoteCredentialsConsumer);

  /**
   * Used to select different credentials. This method should be fast.
   * @return
   */
  @Nonnull
  Object getConnectorKey();
}
