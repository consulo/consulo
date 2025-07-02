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
package consulo.component.impl.internal.messagebus;

import consulo.component.internal.inject.TopicBindingLoader;
import consulo.component.messagebus.MessageBus;
import consulo.component.internal.inject.InjectingContainerOwner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class MessageBusFactory {
  private MessageBusFactory() {}

  public static MessageBusImpl newMessageBus(@Nonnull TopicBindingLoader topicBindingLoader, @Nonnull InjectingContainerOwner owner) {
    return new MessageBusImpl.RootBus(topicBindingLoader, owner);
  }

  public static MessageBusImpl newMessageBus(@Nonnull TopicBindingLoader topicBindingLoader,
                                             @Nonnull InjectingContainerOwner owner,
                                             @Nullable MessageBus parentBus) {
    return parentBus == null ? newMessageBus(topicBindingLoader, owner) : new MessageBusImpl(topicBindingLoader,
                                                                                             owner,
                                                                                             (MessageBusImpl)parentBus);
  }
}