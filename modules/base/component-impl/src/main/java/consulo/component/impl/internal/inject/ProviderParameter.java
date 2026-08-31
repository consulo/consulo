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

import consulo.component.ProviderAsync;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.ui.UIAccess;
import jakarta.inject.Provider;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
class ProviderParameter<T> implements Parameter<Provider<T>> {
  private static class ProviderImpl<T> implements ProviderAsync<T> {
    private final InstanceContainer myContainer;
    private final Class<? super T> myClass;

    private volatile T myValue;

    private ProviderImpl(InstanceContainer container, Class<? super T> aClass) {
      myContainer = container;
      myClass = aClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
      T value = myValue;
      if (value != null) {
        return value;
      }

      // the type check comes first - UIAccess.isUIThread() needs a booted UI environment
      if (isAsyncStateComponent() && UIAccess.isUIThread()) {
        throw new IllegalStateException("Creating " + myClass.getName() + " loads state asynchronously - call #getAsync() on the UI thread");
      }

      value = (T)myContainer.getComponentInstance(myClass);
      myValue = value;
      return value;
    }

    @Override
    public CompletableFuture<T> getAsync() {
      T value = myValue;
      if (value != null) {
        return CompletableFuture.completedFuture(value);
      }

      return myContainer.<T>getComponentInstanceAsync(myClass).thenApply(instance -> {
        myValue = instance;
        return instance;
      });
    }

    private boolean isAsyncStateComponent() {
      ComponentAdapter<T> adapter = myContainer.getComponentAdapter(myClass);
      if (adapter == null) {
        return false;
      }
      Class<?> implClass = adapter.getComponentImplClassIfCheap();
      return implClass != null && PersistentStateComponentAsync.class.isAssignableFrom(implClass);
    }
  }

  private final Class<T> myType;

  ProviderParameter(Class<T> type) {
    myType = type;
  }

  @Override
  public Provider<T> resolveInstance(InstanceContainer picoContainer, ComponentAdapter<Provider<T>> componentAdapter, Class<? super Provider<T>> aClass) {
    return new ProviderImpl<>(picoContainer, myType);
  }

  @Override
  public boolean isResolvable(InstanceContainer picoContainer, ComponentAdapter<Provider<T>> componentAdapter, Class<? super Provider<T>> aClass) {
    return picoContainer.getComponentAdapter(myType) != null;
  }
}
