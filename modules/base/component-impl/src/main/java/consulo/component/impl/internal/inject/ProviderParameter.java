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

import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
class ProviderParameter<T> implements Parameter<Provider<T>> {
  private static class ProviderImpl<T> implements Provider<T> {
    private InstanceContainer myContainer;
    private Class<? super T> myClass;

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
        myContainer = null;
        myClass = null;
        return value;
      }

      value = (T)myContainer.getComponentInstance(myClass);
      myValue = value;
      return value;
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
