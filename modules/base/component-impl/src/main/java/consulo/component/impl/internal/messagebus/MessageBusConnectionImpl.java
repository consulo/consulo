// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.impl.internal.messagebus;

import consulo.annotation.component.TopicAPI;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.SmartFMap;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class MessageBusConnectionImpl implements MessageBusConnection, Disposable {
  private static final Logger LOG = Logger.getInstance(MessageBusConnectionImpl.class);

  private final MessageBusImpl myBus;

  private final ThreadLocal<Queue<Message>> myPendingMessages = MessageBusImpl.createThreadLocalQueue();

  private volatile SmartFMap<Class<?>, Object> mySubscriptions = SmartFMap.emptyMap();

  MessageBusConnectionImpl(@Nonnull MessageBusImpl bus) {
    myBus = bus;
  }

  @Override
  public <L> void subscribe(@Nonnull Class<L> topicClass, @Nonnull L handler) {
    if (!topicClass.isAnnotationPresent(TopicAPI.class)) {
      LOG.error("Registering listener for topic which is not annotated by @TopicAPI " + topicClass);
    }

    boolean notifyBusAboutTopic = false;
    synchronized (myPendingMessages) {
      Object currentHandler = mySubscriptions.get(topicClass);
      if (currentHandler == null) {
        mySubscriptions = mySubscriptions.plus(topicClass, handler);
        notifyBusAboutTopic = true;
      }
      else if (currentHandler instanceof List<?>) {
        //noinspection unchecked
        ((List<L>)currentHandler).add(handler);
      }
      else {
        List<Object> newList = new ArrayList<>();
        newList.add(currentHandler);
        newList.add(handler);
        mySubscriptions = mySubscriptions.plus(topicClass, newList);
      }
    }

    if (notifyBusAboutTopic) {
      myBus.notifyOnSubscription(this, topicClass);
    }
  }

  // avoid notifyOnSubscription and map modification for each handler
  <L> void subscribe(@Nonnull Class<L> topic, @Nonnull List<Object> handlers) {
    boolean notifyBusAboutTopic = false;
    synchronized (myPendingMessages) {
      Object currentHandler = mySubscriptions.get(topic);
      if (currentHandler == null) {
        mySubscriptions = mySubscriptions.plus(topic, handlers);
        notifyBusAboutTopic = true;
      }
      else if (currentHandler instanceof List<?>) {
        //noinspection unchecked
        ((List<Object>)currentHandler).addAll(handlers);
      }
      else {
        List<Object> newList = new ArrayList<>(handlers.size() + 1);
        newList.add(currentHandler);
        newList.addAll(handlers);
        mySubscriptions = mySubscriptions.plus(topic, newList);
      }
    }

    if (notifyBusAboutTopic) {
      myBus.notifyOnSubscription(this, topic);
    }
  }

  @Override
  public void dispose() {
    myPendingMessages.get();
    myPendingMessages.remove();
    myBus.notifyConnectionTerminated(this);
  }

  @Override
  public void disconnect() {
    Disposer.dispose(this);
  }

  @Override
  public void deliverImmediately() {
    Queue<Message> messages = myPendingMessages.get();
    while (!messages.isEmpty()) {
      myBus.deliverSingleMessage();
    }
  }

  void deliverMessage(@Nonnull Message message) {
    final Message messageOnLocalQueue = myPendingMessages.get().poll();
    assert messageOnLocalQueue == message;

    Class<?> topic = message.getTopicClass();
    Object handler = mySubscriptions.get(topic);
    try {
      if (handler instanceof List<?>) {
        for (Object o : (List<?>)handler) {
          myBus.invokeListener(message, o);
        }
      }
      else {
        myBus.invokeListener(message, handler);
      }
    }
    catch (AbstractMethodError e) {
      //Do nothing. This listener just does not implement something newly added yet.
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof ProcessCanceledException) {
        throw (ProcessCanceledException)e.getCause();
      }
      LOG.error(e.getCause() == null ? e : e.getCause());
    }
    catch (Throwable e) {
      LOG.error(e.getCause() == null ? e : e.getCause());
    }
  }

  void scheduleMessageDelivery(@Nonnull Message message) {
    myPendingMessages.get().offer(message);
  }

  boolean containsMessage(@Nonnull Class<?> topic) {
    Queue<Message> pendingMessages = myPendingMessages.get();
    if (pendingMessages.isEmpty()) return false;

    for (Message message : pendingMessages) {
      if (message.getTopicClass() == topic) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return mySubscriptions.toString();
  }

  @Nonnull
  MessageBusImpl getBus() {
    return myBus;
  }
}
