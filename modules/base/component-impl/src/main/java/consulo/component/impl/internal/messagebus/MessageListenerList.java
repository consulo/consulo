// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.impl.internal.messagebus;

import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yole
 */
public class MessageListenerList<T> {
  private final MessageBus myMessageBus;
  private final Class<T> myTopic;
  private final Map<T, MessageBusConnection> myListenerToConnectionMap = new ConcurrentHashMap<>();

  public MessageListenerList(@Nonnull MessageBus messageBus, @Nonnull Class<T> topic) {
    myTopic = topic;
    myMessageBus = messageBus;
  }

  public void add(@Nonnull T listener) {
    final MessageBusConnection connection = myMessageBus.connect();
    connection.subscribe(myTopic, listener);
    myListenerToConnectionMap.put(listener, connection);
  }

  public void add(@Nonnull final T listener, @Nonnull Disposable parentDisposable) {
    Disposer.register(parentDisposable, () -> myListenerToConnectionMap.remove(listener));
    final MessageBusConnection connection = myMessageBus.connect(parentDisposable);
    connection.subscribe(myTopic, listener);
    myListenerToConnectionMap.put(listener, connection);
  }

  public void remove(@Nonnull T listener) {
    final MessageBusConnection connection = myListenerToConnectionMap.remove(listener);
    if (connection != null) {
      connection.disconnect();
    }
  }
}
