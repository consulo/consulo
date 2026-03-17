/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.component.extension;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.component.internal.RootComponentHolder;
import consulo.container.plugin.PluginDescriptor;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author mike
 * <p>
 */
@Deprecated
@DeprecationInfo("Prefer ComponentManager.getExtensionPoint() methods")
public class ExtensionPointName<T> {
  private final Class<? extends T> myIdClass;

  @SuppressWarnings("deprecation")
  public static <T> ExtensionPointName<T> create(Class<? extends T> idClass) {
    return new ExtensionPointName<>(idClass);
  }

  @Deprecated
  @DeprecationInfo("Use #create()")
  public ExtensionPointName(Class<? extends T> idClass) {
    myIdClass = idClass;
  }

  
  public Class<? extends T> getIdClass() {
    return myIdClass;
  }

  
  public String getName() {
    return myIdClass.getName();
  }

  @Override
  public String toString() {
    return myIdClass.toString();
  }

  
  @Deprecated
  public T[] getExtensions() {
    return getExtensions(RootComponentHolder.get());
  }

  
  @Deprecated
  public T[] getExtensions(ComponentManager componentManager) {
    return getExtensionPoint(componentManager).getExtensions();
  }

  public boolean hasAnyExtensions() {
    return hasAnyExtensions(RootComponentHolder.get());
  }

  public boolean hasAnyExtensions(ComponentManager manager) {
    return getExtensionPoint(manager).hasAnyExtensions();
  }

  
  @SuppressWarnings("unchecked")
  private ExtensionPoint<T> getExtensionPoint(ComponentManager componentManager) {
    return componentManager.getExtensionPoint((Class<T>)myIdClass);
  }

  
  @Deprecated
  @DeprecationInfo("Use with component manager")
  public List<T> getExtensionList() {
    return getExtensionList(RootComponentHolder.get());
  }

  
  public List<T> getExtensionList(ComponentManager componentManager) {
    return getExtensionPoint(componentManager).getExtensionList();
  }

  @Nullable
  public <V extends T> V findExtension(Class<V> instanceOf) {
    return findExtension(RootComponentHolder.get(), instanceOf);
  }

  @Nullable
  public <V extends T> V findExtension(ComponentManager componentManager, Class<V> instanceOf) {
    return getExtensionPoint(componentManager).findExtension(instanceOf);
  }

  
  public <V extends T> V findExtensionOrFail(Class<V> instanceOf) {
    return findExtensionOrFail(RootComponentHolder.get(), instanceOf);
  }

  
  public <V extends T> V findExtensionOrFail(ComponentManager componentManager, Class<V> instanceOf) {
    return getExtensionPoint(componentManager).findExtensionOrFail(instanceOf);
  }

  public void forEachExtensionSafe(Consumer<T> consumer) {
    forEachExtensionSafe(RootComponentHolder.get(), consumer);
  }

  public void forEachExtensionSafe(ComponentManager manager, Consumer<T> consumer) {
    getExtensionPoint(manager).forEachExtensionSafe(consumer);
  }

  @Nullable
  public <R> R computeSafeIfAny(Function<? super T, ? extends R> processor) {
    return computeSafeIfAny(RootComponentHolder.get(), processor);
  }

  @Nullable
  public <R> R computeSafeIfAny(ComponentManager componentManager, Function<? super T, ? extends R> processor) {
    return getExtensionPoint(componentManager).computeSafeIfAny(processor);
  }

  @Nullable
  public T findFirstSafe(ComponentManager componentManager, Predicate<T> predicate) {
    return getExtensionPoint(componentManager).findFirstSafe(predicate);
  }

  @Nullable
  public T findFirstSafe(Predicate<T> predicate) {
    return findFirstSafe(RootComponentHolder.get(), predicate);
  }

  public void processWithPluginDescriptor(ComponentManager manager, BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    getExtensionPoint(manager).processWithPluginDescriptor(consumer);
  }

  public void processWithPluginDescriptor(BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    processWithPluginDescriptor(RootComponentHolder.get(), consumer);
  }
}
