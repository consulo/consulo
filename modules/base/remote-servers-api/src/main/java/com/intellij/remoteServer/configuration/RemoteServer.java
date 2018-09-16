package com.intellij.remoteServer.configuration;

import com.intellij.remoteServer.ServerType;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface RemoteServer<C extends ServerConfiguration> {
  @Nonnull
  String getName();

  @Nonnull
  ServerType<C> getType();

  @Nonnull
  C getConfiguration();

  void setName(String name);
}
