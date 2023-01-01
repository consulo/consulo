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
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Oct 22, 2006
 * Time: 9:49:16 PM
 */
package consulo.component.messagebus;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.TopicBroadcastDirection;

import javax.annotation.Nonnull;

/**
 * Defines messaging endpoint within particular {@link MessageBus bus}.
 *
 * @param <L>  type of the interface that defines contract for working with the particular topic instance
 */
@Deprecated
@DeprecationInfo("Use listener class")
public class Topic<L> {
  private final String myDisplayName;
  private final Class<L> myListenerClass;
  private final TopicBroadcastDirection myBroadcastDirection;

  public Topic(@Nonnull Class<L> listenerClass) {
    this(listenerClass.getSimpleName(), listenerClass, TopicBroadcastDirection.TO_CHILDREN);
  }

  public Topic(@Nonnull String displayName, @Nonnull Class<L> listenerClass) {
    this(displayName, listenerClass, TopicBroadcastDirection.TO_CHILDREN);
  }

  public Topic(@Nonnull String displayName, @Nonnull Class<L> listenerClass, final TopicBroadcastDirection broadcastDirection) {
    myDisplayName = displayName;
    myListenerClass = listenerClass;
    myBroadcastDirection = broadcastDirection;
  }

  /**
   * @return    human-readable name of the current topic. Is intended to be used in informational/logging purposes only
   */
  @Nonnull
  public String getDisplayName() {
    return myDisplayName;
  }

  /**
   * Allows to retrieve class that defines contract for working with the current topic. Either publishers or subscribers use it:
   * <ul>
   *   <li>
   *     publisher {@link MessageBus#syncPublisher(Topic) receives} object that IS-A target interface from the messaging infrastructure.
   *     It calls target method with the target arguments on it then (method of the interface returned by the current method);
   *   </li>
   *   <li>
   *     the same method is called on handlers of all {@link MessageBusConnection#subscribe(Topic, Object) subscribers} that
   *     should receive the message;
   *   </li>
   * </ul>
   *
   * @return    class of the interface that defines contract for working with the current topic
   */
  @Nonnull
  public Class<L> getListenerClass() {
    return myListenerClass;
  }

  public String toString() {
    return myDisplayName;
  }

  public static <L> Topic<L> create(@Nonnull String displayName, @Nonnull Class<L> listenerClass) {
    return new Topic<>(displayName, listenerClass);
  }

  public static <L> Topic<L> create(@Nonnull String displayName, @Nonnull Class<L> listenerClass, TopicBroadcastDirection direction) {
    return new Topic<>(displayName, listenerClass, direction);
  }

  /**
   * @return    broadcasting strategy configured for the current topic. Default value is {@link TopicBroadcastDirection#TO_CHILDREN}
   * @see TopicBroadcastDirection
   */
  public TopicBroadcastDirection getBroadcastDirection() {
    return myBroadcastDirection;
  }
}