package com.intellij.remoteServer.configuration;

import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;

import java.util.EventListener;

/**
 * @author nik
 */
public interface RemoteServerListener extends EventListener {
  Topic<RemoteServerListener> TOPIC = Topic.create("remote servers", RemoteServerListener.class);

  void serverAdded(@Nonnull RemoteServer<?> server);
  void serverRemoved(@Nonnull RemoteServer<?> server);
}
