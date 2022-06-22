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
import consulo.component.util.ModificationTracker;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ControlFlowException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link #getModificationCount()} will be changed if plugins reloaded, and cache was changed.
 * Also when count count changed all cache {@link #getOrBuildCache(Key, Function)} & {@link #findExtension(Class)} will be dropped
 *
 * @author AKireyev
 */
public interface ExtensionPoint<T> extends ModificationTracker {
  enum Kind {
    INTERFACE,
    BEAN_CLASS
  }

  @Nonnull
  String getName();

  @Nonnull
  @SuppressWarnings("unchecked")
  @Deprecated
  @DeprecationInfo("Use #getExtensionList()")
  default T[] getExtensions() {
    List<T> list = getExtensionList();
    return list.toArray((T[])Array.newInstance(getExtensionClass(), list.size()));
  }

  default boolean hasAnyExtensions() {
    return !getExtensionList().isEmpty();
  }

  @Nonnull
  List<T> getExtensionList();

  @Nonnull
  Class<T> getExtensionClass();

  @Nonnull
  Kind getKind();

  @Nonnull
  default String getClassName() {
    return getExtensionClass().getName();
  }

  @Override
  default long getModificationCount() {
    return 0;
  }

  @Nullable
  default <K extends T> K findExtension(Class<K> extensionClass) {
    return ContainerUtil.findInstance(getExtensionList(), extensionClass);
  }

  /**
   * Return cache or build it. This cache will be dropped if extensions reloaded (for example plugin added/removed_
   */
  @Nonnull
  default <K> K getOrBuildCache(@Nonnull Key<K> key, @Nonnull Function<List<T>, K> factory) {
    return factory.apply(getExtensionList());
  }

  @Nonnull
  default <V extends T> V findExtensionOrFail(@Nonnull Class<V> instanceOf) {
    V extension = findExtension(instanceOf);
    if (extension == null) {
      throw new IllegalArgumentException("Extension point: " + getName() + " not contains extension of type: " + instanceOf);
    }
    return extension;
  }

  default void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    for (T extension : getExtensionList()) {
      PluginDescriptor plugin = PluginManager.getPlugin(extension.getClass());

      consumer.accept(extension, plugin);
    }
  }

  default void forEachExtensionSafe(@Nonnull Consumer<T> consumer) {
    processWithPluginDescriptor((value, pluginDescriptor) -> {
      try {
        consumer.accept(value);
      }
      catch (Throwable e) {
        if (e instanceof ControlFlowException) {
          throw ControlFlowException.rethrow(e);
        }
        PluginExceptionUtil.logPluginError(Logger.getInstance(ExtensionPoint.class), e.getMessage(), e, value.getClass());
      }
    });
  }
}
