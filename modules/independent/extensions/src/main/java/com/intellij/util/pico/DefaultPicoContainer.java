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
package com.intellij.util.pico;

import com.intellij.openapi.extensions.AreaPicoContainer;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FList;
import gnu.trove.THashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.picocontainer.*;
import org.picocontainer.defaults.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultPicoContainer implements AreaPicoContainer {
  private final PicoContainer parent;
  private final Set<PicoContainer> children = new THashSet<PicoContainer>();

  private final Map<Object, ComponentAdapter> componentKeyToAdapterCache = ContainerUtil.newConcurrentMap();
  private final LinkedHashSetWrapper<ComponentAdapter> componentAdapters = new LinkedHashSetWrapper<ComponentAdapter>();
  private final Map<String, ComponentAdapter> classNameToAdapter = ContainerUtil.newConcurrentMap();
  private final AtomicReference<FList<ComponentAdapter>> nonAssignableComponentAdapters = new AtomicReference<FList<ComponentAdapter>>(FList.<ComponentAdapter>emptyList());

  public DefaultPicoContainer(@javax.annotation.Nullable PicoContainer parent) {
    this.parent = parent == null ? null : ImmutablePicoContainerProxyFactory.newProxyInstance(parent);
  }

  public DefaultPicoContainer() {
    this(null);
  }

  @Override
  public Collection<ComponentAdapter> getComponentAdapters() {
    return componentAdapters.getImmutableSet();
  }

  private void appendNonAssignableAdaptersOfType(@Nonnull Class componentType, @Nonnull List<ComponentAdapter> result) {
    List<ComponentAdapter> comp = new ArrayList<ComponentAdapter>();
    for (final ComponentAdapter componentAdapter : nonAssignableComponentAdapters.get()) {
      if (ReflectionUtil.isAssignable(componentType, componentAdapter.getComponentImplementation())) {
        comp.add(componentAdapter);
      }
    }
    for (int i = comp.size() - 1; i >= 0; i--) {
      result.add(comp.get(i));
    }
  }

  @Override
  @Nullable
  public final ComponentAdapter getComponentAdapter(Object componentKey) {
    ComponentAdapter adapter = getFromCache(componentKey);
    if (adapter == null && parent != null) {
      return parent.getComponentAdapter(componentKey);
    }
    return adapter;
  }

  @javax.annotation.Nullable
  private ComponentAdapter getFromCache(final Object componentKey) {
    ComponentAdapter adapter = componentKeyToAdapterCache.get(componentKey);
    if (adapter != null) {
      return adapter;
    }

    if (componentKey instanceof Class) {
      return componentKeyToAdapterCache.get(((Class)componentKey).getName());
    }

    return null;
  }

  @Override
  @javax.annotation.Nullable
  public ComponentAdapter getComponentAdapterOfType(@Nonnull Class componentType) {
    // See http://jira.codehaus.org/secure/ViewIssue.jspa?key=PICO-115
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

    List<ComponentAdapter> result = new SmartList<ComponentAdapter>();

    final ComponentAdapter cacheHit = classNameToAdapter.get(componentType.getName());
    if (cacheHit != null) {
      result.add(cacheHit);
    }

    appendNonAssignableAdaptersOfType(componentType, result);
    return result;
  }

  @Override
  public ComponentAdapter registerComponent(@Nonnull ComponentAdapter componentAdapter) {
    Object componentKey = componentAdapter.getComponentKey();
    if (componentKeyToAdapterCache.containsKey(componentKey)) {
      throw new DuplicateComponentKeyRegistrationException(componentKey);
    }

    if (componentAdapter instanceof AssignableToComponentAdapter) {
      String classKey = ((AssignableToComponentAdapter)componentAdapter).getAssignableToClassName();
      classNameToAdapter.put(classKey, componentAdapter);
    }
    else {
      do {
        FList<ComponentAdapter> oldList = nonAssignableComponentAdapters.get();
        FList<ComponentAdapter> newList = oldList.prepend(componentAdapter);
        if (nonAssignableComponentAdapters.compareAndSet(oldList, newList)) {
          break;
        }
      }
      while (true);
    }

    componentAdapters.add(componentAdapter);

    componentKeyToAdapterCache.put(componentKey, componentAdapter);
    return componentAdapter;
  }

  @Override
  public ComponentAdapter unregisterComponent(@Nonnull Object componentKey) {
    ComponentAdapter adapter = componentKeyToAdapterCache.remove(componentKey);
    componentAdapters.remove(adapter);
    if (adapter instanceof AssignableToComponentAdapter) {
      classNameToAdapter.remove(((AssignableToComponentAdapter)adapter).getAssignableToClassName());
    }
    else {
      do {
        FList<ComponentAdapter> oldList = nonAssignableComponentAdapters.get();
        FList<ComponentAdapter> newList = oldList.without(adapter);
        if (nonAssignableComponentAdapters.compareAndSet(oldList, newList)) {
          break;
        }
      }
      while (true);
    }
    return adapter;
  }

  @Override
  public List getComponentInstances() {
    return getComponentInstancesOfType(Object.class);
  }

  @Override
  public List<Object> getComponentInstancesOfType(@javax.annotation.Nullable Class componentType) {
    if (componentType == null) {
      return Collections.emptyList();
    }

    List<Object> result = new ArrayList<Object>();
    for (ComponentAdapter componentAdapter : getComponentAdapters()) {
      if (ReflectionUtil.isAssignable(componentType, componentAdapter.getComponentImplementation())) {
        // may be null in the case of the "implicit" adapter representing "this".
        ContainerUtil.addIfNotNull(result, getInstance(componentAdapter));
      }
    }
    return result;
  }

  @Override
  @javax.annotation.Nullable
  public Object getComponentInstance(Object componentKey) {
    ComponentAdapter adapter = getFromCache(componentKey);
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
  @javax.annotation.Nullable
  public Object getComponentInstanceOfType(Class componentType) {
    final ComponentAdapter componentAdapter = getComponentAdapterOfType(componentType);
    return componentAdapter == null ? null : getInstance(componentAdapter);
  }

  @javax.annotation.Nullable
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
    catch (PicoInitializationException e) {
      firstLevelException = e;
    }
    catch (PicoIntrospectionException e) {
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
  @javax.annotation.Nullable
  public ComponentAdapter unregisterComponentByInstance(@Nonnull Object componentInstance) {
    for (ComponentAdapter adapter : getComponentAdapters()) {
      Object o = getInstance(adapter);
      if (o != null && o.equals(componentInstance)) {
        return unregisterComponent(adapter.getComponentKey());
      }
    }
    return null;
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
    throw new UnsupportedOperationException();
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
    return children.add(child);
  }

  @Override
  public boolean removeChildContainer(@Nonnull PicoContainer child) {
    return children.remove(child);
  }

  @Override
  public void accept(PicoVisitor visitor) {
    visitor.visitContainer(this);

    for (ComponentAdapter adapter : getComponentAdapters()) {
      adapter.accept(visitor);
    }
    for (PicoContainer child : new SmartList<PicoContainer>(children)) {
      child.accept(visitor);
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
    ComponentAdapter componentAdapter = new CachingConstructorInjectionComponentAdapter(componentKey, componentImplementation, parameters, true);
    return registerComponent(componentAdapter);
  }

  @Override
  public PicoContainer getParent() {
    return parent;
  }

  /**
   * A linked hash set that's copied on write operations.
   * @param <T>
   */
  private static class LinkedHashSetWrapper<T> {
    private final Object lock = new Object();
    private volatile Set<T> immutableSet;
    private LinkedHashSet<T> synchronizedSet = new LinkedHashSet<T>();

    public void add(@Nonnull T element) {
      synchronized (lock) {
        if (!synchronizedSet.contains(element)) {
          copySyncSetIfExposedAsImmutable().add(element);
        }
      }
    }

    private LinkedHashSet<T> copySyncSetIfExposedAsImmutable() {
      if (immutableSet != null) {
        immutableSet = null;
        synchronizedSet = new LinkedHashSet<T>(synchronizedSet);
      }
      return synchronizedSet;
    }

    public void remove(@javax.annotation.Nullable T element) {
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
    return "DefaultPicoContainer" + (getParent() == null ? " (root)" : " (parent="+getParent()+")");
  }
}