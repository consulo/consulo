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
import consulo.annotation.component.ExtensionImpl;
import consulo.component.internal.ExtensionLogger;
import consulo.component.util.ModificationTracker;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link #getModificationCount()} will be changed if plugins reloaded, and cache was changed.
 * Also when count count changed all cache {@link #getOrBuildCache(ExtensionPointCacheKey)} & {@link #findExtension(Class)} will be dropped
 */
public interface ExtensionPoint<E> extends ModificationTracker, Iterable<E> {
    @Nonnull
    default String getName() {
        return getClassName();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    @Deprecated
    @DeprecationInfo("Use #getExtensionList()")
    default E[] getExtensions() {
        List<E> list = getExtensionList();
        return list.toArray((E[])Array.newInstance(getExtensionClass(), list.size()));
    }

    default boolean hasAnyExtensions() {
        return !getExtensionList().isEmpty();
    }

    @Nonnull
    List<E> getExtensionList();

    @Nonnull
    Class<E> getExtensionClass();

    @Nonnull
    default String getClassName() {
        return getExtensionClass().getName();
    }

    @Override
    default long getModificationCount() {
        return 0;
    }

    @Nullable
    default <K extends E> K findExtension(Class<K> extensionClass) {
        return ContainerUtil.findInstance(getExtensionList(), extensionClass);
    }

    /**
     * Sort extensions depends on @{@link ExtensionImpl#order()} - internal logic
     */
    @Nonnull
    default List<E> sort(List<E> extensionsList) {
        return extensionsList;
    }

    /**
     * Return cache or build it. This cache will be dropped if extensions reloaded (for example plugin added/removed)
     */
    @Nonnull
    <K> K getOrBuildCache(@Nonnull ExtensionPointCacheKey<E, K> key);

    @Nonnull
    default <V extends E> V findExtensionOrFail(@Nonnull Class<V> instanceOf) {
        V extension = findExtension(instanceOf);
        if (extension == null) {
            throw new IllegalArgumentException("Extension point: " + getName() + " not contains extension of type: " + instanceOf);
        }
        return extension;
    }

    default void processWithPluginDescriptor(@Nonnull BiConsumer<? super E, ? super PluginDescriptor> consumer) {
        for (E extension : getExtensionList()) {
            PluginDescriptor plugin = PluginManager.getPlugin(extension.getClass());

            consumer.accept(extension, plugin);
        }
    }

    default boolean anyMatchSafe(@Nonnull Predicate<E> predicate) {
        return findFirstSafe(predicate) != null;
    }

    @Nullable
    default E findFirstSafe(@Nonnull Predicate<E> predicate) {
        return computeSafeIfAny(e -> predicate.test(e) ? e : null);
    }

    @Nullable
    default <R> R computeSafeIfAny(@Nonnull Function<? super E, ? extends R> processor) {
        for (E extension : getExtensionList()) {
            try {
                R result = processor.apply(extension);
                if (result != null) {
                    return result;
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, extension);
            }
        }
        return null;
    }

    @Nonnull
    default <R> R computeSafeIfAny(@Nonnull Function<? super E, ? extends R> processor, @Nonnull R defaultValue) {
        R result = computeSafeIfAny(processor);
        return result == null ? defaultValue : result;
    }

    @Nonnull
    default <R, CR extends Collection<? super R>> CR collectExtensionsSafe(
        @Nonnull CR results,
        @Nonnull Function<? super E, ? extends R> processor
    ) {
        forEach(extension -> {
            R result = processor.apply(extension);
            if (result != null) {
                results.add(result);
            }
        });
        return results;
    }

    @Nonnull
    default <R> List<R> collectExtensionsToListSafe(@Nonnull Function<? super E, ? extends R> processor) {
        return collectExtensionsSafe(new ArrayList<R>(), processor);
    }

    @Override
    default void forEach(Consumer<? super E> action) {
        forEachExtensionSafe(action);
    }

    default void forEachExtensionSafe(@Nonnull Consumer<? super E> consumer) {
        processWithPluginDescriptor((value, pluginDescriptor) -> {
            try {
                consumer.accept(value);
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, value);
            }
        });
    }

    @Override
    default Iterator<E> iterator() {
        return getExtensionList().iterator();
    }
}
