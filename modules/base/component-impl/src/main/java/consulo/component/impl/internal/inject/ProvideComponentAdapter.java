/*
 * Copyright 2013-2018 consulo.io
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
package consulo.component.impl.internal.inject;


import consulo.component.internal.inject.InjectingKey;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
class ProvideComponentAdapter<T> implements ComponentAdapter<T> {
  private final InjectingKey<T> myInterfaceKey;
  private final Provider<T> myValue;

  public ProvideComponentAdapter(InjectingKey<T> interfaceKey, Provider<T> value) {
    myInterfaceKey = interfaceKey;
    myValue = value;
  }

  @Nonnull
  @Override
  public Class getComponentClass() {
    return myInterfaceKey.getTargetClass();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<T> getComponentImplClass() {
    return (Class<T>)myValue.get().getClass();
  }

  @Override
  public T getComponentInstance(@Nonnull InstanceContainer container) throws PicoInitializationException, PicoIntrospectionException {
    return myValue.get();
  }

  @Override
  public String toString() {
    return "ProvideComponentAdapter[" + myInterfaceKey.getTargetClassName() + "]";
  }
}