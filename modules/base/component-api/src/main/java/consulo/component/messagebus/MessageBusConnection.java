/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author max
 */
package consulo.component.messagebus;

import jakarta.annotation.Nonnull;

/**
 * Aggregates multiple topic subscriptions for particular {@link MessageBus message bus}. I.e. every time a client wants to
 * listen for messages it should grab appropriate connection (or create a new one) and {@link #subscribe(Class, Object) subscribe}
 * to particular endpoint.
 */
public interface MessageBusConnection {

  @Deprecated
  default <L> void subscribe(@Nonnull Topic<L> topic, @Nonnull L handler) throws IllegalStateException {
    subscribe(topic.getListenerClass(), handler);
  }

  /**
   * Subscribes given handler to the target endpoint within the current connection.
   *
   * @param topicClass   target endpoint
   * @param handler target handler to use for incoming messages
   * @param <L>     interface for working with the target topic
   * @throws IllegalStateException if there is already registered handler for the target endpoint within the current connection.
   *                               Note that that previously registered handler is not replaced by the given one then
   * @see MessageBus#syncPublisher(Class)
   */
  <L> void subscribe(@Nonnull Class<L> topicClass, @Nonnull L handler) throws IllegalStateException;

  /**
   * Forces to process any queued but not delivered events.
   *
   * @see MessageBus#syncPublisher(Class)
   */
  void deliverImmediately();

  /**
   * Disconnects current connections from the {@link MessageBus message bus} and drops all queued but not dispatched messages (if any)
   */
  void disconnect();
}
