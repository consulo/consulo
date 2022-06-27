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

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

class InstanceContainer {
  private final InstanceContainer myParent;

  private final Map<Class, ComponentAdapter> myInstanceAdapters = Maps.newHashMap(HashingStrategy.identity());

  InstanceContainer(@Nullable InstanceContainer parent) {
    myParent = parent;
  }

  @Nullable
  public final <T> ComponentAdapter<T> getComponentAdapter(Class componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter == null && myParent != null) {
      return myParent.getComponentAdapter(componentKey);
    }
    return adapter;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> ComponentAdapter<T> findLocalAdapter(final Class componentKey) {
    return (ComponentAdapter<T>) myInstanceAdapters.get(componentKey);
  }

  @Nullable
  public <T> T getComponentInstance(Class componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter != null) {
      return getLocalInstance(adapter);
    }
    if (myParent != null) {
      adapter = myParent.getComponentAdapter(componentKey);
      if (adapter != null) {
        return myParent.getComponentInstance(adapter.getComponentClass());
      }
    }
    return null;
  }

  @Nullable
  public <T> T getComponentInstanceIfCreated(Class componentKey) {
    ComponentAdapter<T> adapter = findLocalAdapter(componentKey);
    if (adapter != null) {
      return adapter.getComponentInstanceOfCreated(this);
    }
    if (myParent != null) {
      return myParent.getComponentInstanceIfCreated(componentKey);
    }
    return null;
  }

  private boolean contains(Class componentKey) {
    return myInstanceAdapters.containsKey(componentKey);
  }

  public void registerComponent(@Nonnull ComponentAdapter componentAdapter) {
    Class componentKey = componentAdapter.getComponentClass();

    if (contains(componentKey)) {
      throw new DuplicateComponentKeyRegistrationException(componentKey);
    }

    myInstanceAdapters.put(componentKey, componentAdapter);
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
      T instance = myParent.getComponentInstance(componentAdapter.getComponentClass());
      if (instance != null) {
        return instance;
      }
    }

    throw firstLevelException;
  }

  public void dispose() {
    myInstanceAdapters.clear();
  }
}