/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.injecting.pico;

import gnu.trove.TIntObjectHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class DefaultPicoContainer {
  private final DefaultPicoContainer myParent;

  private final TIntObjectHashMap<ComponentAdapter> myClassNameToComponentAdapters = new TIntObjectHashMap<>();

  DefaultPicoContainer(@Nullable DefaultPicoContainer parent) {
    myParent = parent;
  }

  @Nullable
  public final <T> ComponentAdapter<T> getComponentAdapter(Object componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter == null && myParent != null) {
      return myParent.getComponentAdapter(componentKey);
    }
    return adapter;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> ComponentAdapter<T> findLocalAdapter(final Object componentKey) {
    return (ComponentAdapter<T>) myClassNameToComponentAdapters.get(toMapKey(componentKey));
  }

  @Nullable
  public <T> T getComponentInstance(Object componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter != null) {
      return getLocalInstance(adapter);
    }
    if (myParent != null) {
      adapter = myParent.getComponentAdapter(componentKey);
      if (adapter != null) {
        return myParent.getComponentInstance(adapter.getComponentKey());
      }
    }
    return null;
  }

  @Nullable
  public <T> T getComponentInstanceIfCreated(Object componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter != null) {
      return adapter.getComponentInstanceOfCreated(this);
    }
    if (myParent != null) {
      return myParent.getComponentInstanceIfCreated(componentKey);
    }
    return null;
  }

  private boolean contains(int key) {
    return myClassNameToComponentAdapters.containsKey(key) || myParent != null && myParent.contains(key);
  }

  public void registerComponent(@Nonnull ComponentAdapter componentAdapter) {
    int componentKey = toMapKey(componentAdapter.getComponentKey());

    if (contains(componentKey)) {
      throw new DuplicateComponentKeyRegistrationException(componentKey);
    }

    myClassNameToComponentAdapters.put(componentKey, componentAdapter);
  }

  private int toMapKey(Object value) {
    if (value instanceof String) {
      return value.hashCode();
    }

    if (value instanceof Class) {
      return ((Class)value).getName().hashCode();
    }

    throw new UnsupportedOperationException("Unknown key type " + value);
  }

  private <T> T getLocalInstance(@Nonnull ComponentAdapter<T> componentAdapter) {
    PicoException firstLevelException;
    try {
      return componentAdapter.getComponentInstance(this);
    }
    catch (PicoInitializationException | PicoIntrospectionException e) {
      firstLevelException = e;
    }

    if (myParent != null) {
      T instance = myParent.getComponentInstance(componentAdapter.getComponentKey());
      if (instance != null) {
        return instance;
      }
    }

    throw firstLevelException;
  }

  public void dispose() {
    myClassNameToComponentAdapters.clear();
  }

  public DefaultPicoContainer getParent() {
    return myParent;
  }
}