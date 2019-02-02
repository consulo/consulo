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

import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.picocontainer.*;
import org.picocontainer.defaults.AmbiguousComponentResolutionException;
import org.picocontainer.defaults.DuplicateComponentKeyRegistrationException;
import org.picocontainer.defaults.VerifyingVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class DefaultPicoContainer implements PicoContainer {
  private final PicoContainer myParent;

  private final Map<String, ComponentAdapter> myInterfaceClassToAdapter = new THashMap<>();

  private final List<ComponentAdapter> myComponentAdapters = new ArrayList<>();

  DefaultPicoContainer(@Nullable PicoContainer parent) {
    myParent = parent;
  }

  @Override
  public Collection<ComponentAdapter> getComponentAdapters() {
    return myComponentAdapters;
  }

  @Override
  @Nullable
  public final ComponentAdapter getComponentAdapter(Object componentKey) {
    ComponentAdapter adapter = findByKey(componentKey);
    if (adapter == null && myParent != null) {
      return myParent.getComponentAdapter(componentKey);
    }
    return adapter;
  }

  @Nullable
  private ComponentAdapter findByKey(final Object componentKey) {
    if (componentKey instanceof String) {
      ComponentAdapter adapter = myInterfaceClassToAdapter.get(componentKey);
      if (adapter != null) {
        return adapter;
      }
    }

    if (componentKey instanceof Class) {
      return myInterfaceClassToAdapter.get(((Class)componentKey).getName());
    }

    return null;
  }

  @Override
  @Nullable
  public ComponentAdapter getComponentAdapterOfType(@Nonnull Class componentType) {
    ComponentAdapter adapterByKey = getComponentAdapter(componentType);
    if (adapterByKey != null) {
      return adapterByKey;
    }

    List<ComponentAdapter> found = getComponentAdaptersOfType(componentType);
    if (found.size() == 1) {
      return found.get(0);
    }
    if (found.isEmpty()) {
      return myParent == null ? null : myParent.getComponentAdapterOfType(componentType);
    }

    Class[] foundClasses = new Class[found.size()];
    for (int i = 0; i < foundClasses.length; i++) {
      foundClasses[i] = found.get(i).getComponentImplementation();
    }
    throw new AmbiguousComponentResolutionException(componentType, foundClasses);
  }

  @Override
  public List<ComponentAdapter> getComponentAdaptersOfType(final Class componentType) {
    if (componentType == null || componentType == String.class) {
      return Collections.emptyList();
    }

    List<ComponentAdapter> result = new SmartList<>();

    final ComponentAdapter cacheHit = myInterfaceClassToAdapter.get(componentType.getName());
    if (cacheHit != null) {
      result.add(cacheHit);
    }

    return result;
  }

  private boolean contains(String key) {
    return myInterfaceClassToAdapter.containsKey(key) || myParent instanceof DefaultPicoContainer && ((DefaultPicoContainer)myParent).contains(key);
  }

  public ComponentAdapter registerComponent(@Nonnull ComponentAdapter componentAdapter) {
    String componentKey = toKey(componentAdapter.getComponentKey());

    if (contains(componentKey)) {
      throw new DuplicateComponentKeyRegistrationException(componentKey);
    }

    myInterfaceClassToAdapter.put(componentKey, componentAdapter);
    myComponentAdapters.add(componentAdapter);
    return componentAdapter;
  }

  private String toKey(Object value) {
    if (value instanceof String) {
      return (String)value;
    }

    if (value instanceof Class) {
      return ((Class)value).getName();
    }

    throw new UnsupportedOperationException("Unknown key type " + value);
  }

  @Override
  public List getComponentInstances() {
    return getComponentInstancesOfType(Object.class);
  }

  @Override
  public List<Object> getComponentInstancesOfType(@Nullable Class componentType) {
    if (componentType == null) {
      return Collections.emptyList();
    }

    List<Object> result = new ArrayList<>();
    for (ComponentAdapter componentAdapter : getComponentAdapters()) {
      if (ReflectionUtil.isAssignable(componentType, componentAdapter.getComponentImplementation())) {
        // may be null in the case of the "implicit" adapter representing "this".
        ContainerUtil.addIfNotNull(result, getInstance(componentAdapter));
      }
    }
    return result;
  }

  @Override
  @Nullable
  public Object getComponentInstance(Object componentKey) {
    ComponentAdapter adapter = findByKey(componentKey);
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

  @Override
  @Nullable
  public Object getComponentInstanceOfType(Class componentType) {
    final ComponentAdapter componentAdapter = getComponentAdapterOfType(componentType);
    return componentAdapter == null ? null : getInstance(componentAdapter);
  }

  @Nullable
  private Object getInstance(@Nonnull ComponentAdapter componentAdapter) {
    if (getComponentAdapters().contains(componentAdapter)) {
      return getLocalInstance(componentAdapter);
    }
    if (myParent != null) {
      return myParent.getComponentInstance(componentAdapter.getComponentKey());
    }

    return null;
  }

  private Object getLocalInstance(@Nonnull ComponentAdapter componentAdapter) {
    PicoException firstLevelException;
    try {
      return componentAdapter.getComponentInstance(this);
    }
    catch (PicoInitializationException | PicoIntrospectionException e) {
      firstLevelException = e;
    }

    if (myParent != null) {
      Object instance = myParent.getComponentInstance(componentAdapter.getComponentKey());
      if (instance != null) {
        return instance;
      }
    }

    throw firstLevelException;
  }

  @Override
  public void verify() {
    new VerifyingVisitor().traverse(this);
  }

  @Override
  public void start() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stop() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() {
    myInterfaceClassToAdapter.clear();
    myComponentAdapters.clear();
  }

  @Override
  public void accept(PicoVisitor visitor) {
    visitor.visitContainer(this);

    for (ComponentAdapter adapter : getComponentAdapters()) {
      adapter.accept(visitor);
    }
  }

  @Override
  public PicoContainer getParent() {
    return myParent;
  }
}