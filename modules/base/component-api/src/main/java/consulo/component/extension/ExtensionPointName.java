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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
  public static <T> ExtensionPointName<T> create(@Nonnull Class<? extends T> idClass) {
    return new ExtensionPointName<>(idClass);
  }

  @Deprecated
  @DeprecationInfo("Use #create()")
  public ExtensionPointName(@Nonnull Class<? extends T> idClass) {
    myIdClass = idClass;
  }

  @Nonnull
  public Class<? extends T> getIdClass() {
    return myIdClass;
  }

  @Nonnull
  public String getName() {
    return myIdClass.getName();
  }

  @Override
  public String toString() {
    return myIdClass.toString();
  }

  @Nonnull
  @Deprecated
  public T[] getExtensions() {
    return getExtensions(RootComponentHolder.getRootComponent());
  }

  @Nonnull
  @Deprecated
  public T[] getExtensions(@Nonnull ComponentManager componentManager) {
    return getExtensionPoint(componentManager).getExtensions();
  }

  public boolean hasAnyExtensions() {
    return hasAnyExtensions(RootComponentHolder.getRootComponent());
  }

  public boolean hasAnyExtensions(@Nonnull ComponentManager manager) {
    return getExtensionPoint(manager).hasAnyExtensions();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private ExtensionPoint<T> getExtensionPoint(@Nonnull ComponentManager componentManager) {
    return componentManager.getExtensionPoint((Class<T>)myIdClass);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use with component manager")
  public List<T> getExtensionList() {
    return getExtensionList(RootComponentHolder.getRootComponent());
  }

  @Nonnull
  public List<T> getExtensionList(@Nonnull ComponentManager componentManager) {
    return getExtensionPoint(componentManager).getExtensionList();
  }

  @Nullable
  public <V extends T> V findExtension(@Nonnull Class<V> instanceOf) {
    return findExtension(RootComponentHolder.getRootComponent(), instanceOf);
  }

  @Nullable
  public <V extends T> V findExtension(@Nonnull ComponentManager componentManager, @Nonnull Class<V> instanceOf) {
    return getExtensionPoint(componentManager).findExtension(instanceOf);
  }

  @Nonnull
  public <V extends T> V findExtensionOrFail(@Nonnull Class<V> instanceOf) {
    return findExtensionOrFail(RootComponentHolder.getRootComponent(), instanceOf);
  }

  @Nonnull
  public <V extends T> V findExtensionOrFail(@Nonnull ComponentManager componentManager, @Nonnull Class<V> instanceOf) {
    return getExtensionPoint(componentManager).findExtensionOrFail(instanceOf);
  }

  public void forEachExtensionSafe(@Nonnull Consumer<T> consumer) {
    forEachExtensionSafe(RootComponentHolder.getRootComponent(), consumer);
  }

  public void forEachExtensionSafe(@Nonnull ComponentManager manager, @Nonnull Consumer<T> consumer) {
    getExtensionPoint(manager).forEachExtensionSafe(consumer);
  }

  @Nullable
  public <R> R computeSafeIfAny(@Nonnull Function<? super T, ? extends R> processor) {
    return computeSafeIfAny(RootComponentHolder.getRootComponent(), processor);
  }

  @Nullable
  public <R> R computeSafeIfAny(@Nonnull ComponentManager componentManager, @Nonnull Function<? super T, ? extends R> processor) {
    return getExtensionPoint(componentManager).computeSafeIfAny(processor);
  }

  @Nullable
  public T findFirstSafe(@Nonnull ComponentManager componentManager, @Nonnull Predicate<T> predicate) {
    return getExtensionPoint(componentManager).findFirstSafe(predicate);
  }

  @Nullable
  public T findFirstSafe(@Nonnull Predicate<T> predicate) {
    return findFirstSafe(RootComponentHolder.getRootComponent(), predicate);
  }

  public void processWithPluginDescriptor(@Nonnull ComponentManager manager, @Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    getExtensionPoint(manager).processWithPluginDescriptor(consumer);
  }

  public void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    processWithPluginDescriptor(RootComponentHolder.getRootComponent(), consumer);
  }
}
