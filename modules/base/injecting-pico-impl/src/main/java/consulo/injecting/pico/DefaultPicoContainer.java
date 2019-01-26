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
import org.picocontainer.defaults.InstanceComponentAdapter;
import org.picocontainer.defaults.VerifyingVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class DefaultPicoContainer implements MutablePicoContainer {
  private final PicoContainer parent;

  private final Map<String, ComponentAdapter> myInterfaceClassToAdapter = new THashMap<>();

  private final LinkedHashSetWrapper<ComponentAdapter> myComponentAdapters = new LinkedHashSetWrapper<>();

  public DefaultPicoContainer(@Nullable PicoContainer parent) {
    this.parent = parent == null ? null : parent;
  }

  @Override
  public Collection<ComponentAdapter> getComponentAdapters() {
    return myComponentAdapters.getImmutableSet();
  }

  @Override
  @Nullable
  public final ComponentAdapter getComponentAdapter(Object componentKey) {
    ComponentAdapter adapter = findByKey(componentKey);
    if (adapter == null && parent != null) {
      return parent.getComponentAdapter(componentKey);
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
      return parent == null ? null : parent.getComponentAdapterOfType(componentType);
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
    return myInterfaceClassToAdapter.containsKey(key) || parent instanceof DefaultPicoContainer && ((DefaultPicoContainer)parent).contains(key);
  }

  @Override
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
  public ComponentAdapter unregisterComponent(@Nonnull Object componentKey) {
    throw new UnsupportedOperationException();
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
    if (parent != null) {
      adapter = parent.getComponentAdapter(componentKey);
      if (adapter != null) {
        return parent.getComponentInstance(adapter.getComponentKey());
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
    if (parent != null) {
      return parent.getComponentInstance(componentAdapter.getComponentKey());
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

    if (parent != null) {
      Object instance = parent.getComponentInstance(componentAdapter.getComponentKey());
      if (instance != null) {
        return instance;
      }
    }

    throw firstLevelException;
  }

  @Override
  @Nullable
  public ComponentAdapter unregisterComponentByInstance(@Nonnull Object componentInstance) {
    throw new UnsupportedOperationException();
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

  @Nonnull
  @Override
  public MutablePicoContainer makeChildContainer() {
    DefaultPicoContainer pc = new DefaultPicoContainer(this);
    addChildContainer(pc);
    return pc;
  }

  @Override
  public boolean addChildContainer(@Nonnull PicoContainer child) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeChildContainer(@Nonnull PicoContainer child) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(PicoVisitor visitor) {
    visitor.visitContainer(this);

    for (ComponentAdapter adapter : getComponentAdapters()) {
      adapter.accept(visitor);
    }
  }

  @Override
  public ComponentAdapter registerComponentInstance(@Nonnull Object component) {
    return registerComponentInstance(component.getClass(), component);
  }

  @Override
  public ComponentAdapter registerComponentInstance(@Nonnull Object componentKey, @Nonnull Object componentInstance) {
    return registerComponent(new InstanceComponentAdapter(componentKey, componentInstance));
  }

  @Override
  public ComponentAdapter registerComponentImplementation(@Nonnull Class componentImplementation) {
    return registerComponentImplementation(componentImplementation, componentImplementation);
  }

  @Override
  public ComponentAdapter registerComponentImplementation(@Nonnull Object componentKey, @Nonnull Class componentImplementation) {
    return registerComponentImplementation(componentKey, componentImplementation, null);
  }

  @Override
  public ComponentAdapter registerComponentImplementation(@Nonnull Object componentKey, @Nonnull Class componentImplementation, Parameter[] parameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PicoContainer getParent() {
    return parent;
  }

  /**
   * A linked hash set that's copied on write operations.
   *
   * @param <T>
   */
  private static class LinkedHashSetWrapper<T> {
    private final Object lock = new Object();
    private volatile Set<T> immutableSet;
    private LinkedHashSet<T> synchronizedSet = new LinkedHashSet<>();

    public void add(@Nonnull T element) {
      synchronized (lock) {
        if (!synchronizedSet.contains(element)) {
          copySyncSetIfExposedAsImmutable().add(element);
        }
      }
    }

    public void clear() {
      synchronized (lock) {
        immutableSet = null;
        synchronizedSet.clear();
      }
    }

    private LinkedHashSet<T> copySyncSetIfExposedAsImmutable() {
      if (immutableSet != null) {
        immutableSet = null;
        synchronizedSet = new LinkedHashSet<>(synchronizedSet);
      }
      return synchronizedSet;
    }

    public void remove(@Nullable T element) {
      synchronized (lock) {
        copySyncSetIfExposedAsImmutable().remove(element);
      }
    }

    @Nonnull
    public Set<T> getImmutableSet() {
      Set<T> res = immutableSet;
      if (res == null) {
        synchronized (lock) {
          res = immutableSet;
          if (res == null) {
            // Expose the same set as immutable. It should be never modified again. Next add/remove operations will copy synchronizedSet
            immutableSet = res = Collections.unmodifiableSet(synchronizedSet);
          }
        }
      }

      return res;
    }
  }

  @Override
  public String toString() {
    return "DefaultPicoContainer" + (getParent() == null ? " (root)" : " (parent=" + getParent() + ")");
  }
}