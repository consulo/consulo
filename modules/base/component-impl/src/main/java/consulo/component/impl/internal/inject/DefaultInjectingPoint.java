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
import consulo.component.internal.inject.InjectingPoint;
import consulo.component.internal.inject.PostInjectListener;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
class DefaultInjectingPoint<T> implements InjectingPoint<T> {
  private final InjectingKey<T> myKey;

  private boolean myLocked;

  private ComponentAdapter myAdapter;

  public DefaultInjectingPoint(InjectingKey<T> key) {
    myKey = key;
    // map to self
    myAdapter = new BaseComponentAdapter<>(key);
  }

  @Nonnull
  @Override
  public InjectingPoint<T> to(@Nonnull T value) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    myLocked = true;
    myAdapter = new ProvideComponentAdapter<>(myKey, () -> value);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> to(@Nonnull InjectingKey<? extends T> key) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    myLocked = true;
    base().setImplementationKey(key);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> to(@Nonnull Provider<T> provider) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    myLocked = true;
    myAdapter = new ProvideComponentAdapter<>(myKey, provider);
    return this;
  }

  @Override
  public InjectingPoint<T> forceSingleton() {
    base().setForceSingleton();
    return this;
  }

  @Override
  public InjectingPoint<T> factory(@Nonnull Function<Provider<T>, T> remap) {
    base().setRemap(remap);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> injectListener(@Nonnull PostInjectListener<T> consumer) {
    base().setAfterInjectionListener(consumer);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> constructorParameterTypes(@Nonnull Type[] constructorParameterTypes) {
    base().setConstructorParameterTypes(constructorParameterTypes);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> constructorFactory(@Nonnull Function<Object[], T> factory) {
    base().setConstructorFactory(factory);
    return this;
  }

  @SuppressWarnings("unchecked")
  private BaseComponentAdapter<T> base() {
    if (!(myAdapter instanceof BaseComponentAdapter)) {
      throw new IllegalArgumentException("Wrong instance provider");
    }

    return (BaseComponentAdapter<T>)myAdapter;
  }

  public ComponentAdapter getAdapter() {
    return myAdapter;
  }
}
