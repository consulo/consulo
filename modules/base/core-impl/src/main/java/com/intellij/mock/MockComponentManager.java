/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.mock;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.impl.MessageBusFactory;
import consulo.disposer.Disposable;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class MockComponentManager extends UserDataHolderBase implements ComponentManager {
  private final MessageBus myMessageBus = MessageBusFactory.newMessageBus(this);

  private final Map<Class, Object> myComponents = new HashMap<Class, Object>();

  public MockComponentManager(@Nullable MockComponentManager parent, @Nonnull Disposable parentDisposable) {

  }

  public <T> void registerService(@Nonnull Class<T> serviceInterface, @Nonnull Class<? extends T> serviceImplementation) {

  }

  public <T> void registerService(@Nonnull Class<T> serviceImplementation) {
    registerService(serviceImplementation, serviceImplementation);
  }

  public <T> void registerService(@Nonnull Class<T> serviceInterface, @Nonnull T serviceImplementation) {
  }

  public <T> void addComponent(@Nonnull Class<T> interfaceClass, @Nonnull T instance) {
    myComponents.put(interfaceClass, instance);
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> clazz) {
    return null;
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    return myMessageBus;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dispose() {
  }

  @Nonnull
  @Override
  public Condition getDisposed() {
    return Conditions.alwaysFalse();
  }
}
