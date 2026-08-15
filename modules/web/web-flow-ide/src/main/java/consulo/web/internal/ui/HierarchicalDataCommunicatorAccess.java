/*
 * Copyright 2013-2026 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataCommunicator;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A row of a tree grid answers the flat index it is drawn at, and that index is what the communicator keeps
 * its cache by - but {@link HierarchicalDataCommunicator} publishes nothing that takes one.
 * {@code DataCommunicator#getItem(int)} is the flat lookup of the non hierarchical case and is not overridden,
 * so it walks the wrong cache. The cache that does answer is package private, hence the reflection here:
 * it reads the item and nothing else.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public final class HierarchicalDataCommunicatorAccess {
    private static final Field ROOT_CACHE;
    private static final Method GET_FLAT_SIZE;
    private static final Method GET_CONTEXT_BY_FLAT_INDEX;
    private static final Method CONTEXT_CACHE;
    private static final Method CONTEXT_INDEX;
    private static final Method HAS_ITEM;
    private static final Method GET_ITEM;

    static {
        try {
            ClassLoader classLoader = HierarchicalDataCommunicator.class.getClassLoader();
            Class<?> rootCacheClass = Class.forName("com.vaadin.flow.data.provider.hierarchy.RootCache", false, classLoader);
            Class<?> itemContextClass = Class.forName("com.vaadin.flow.data.provider.hierarchy.RootCache$ItemContext", false, classLoader);
            Class<?> cacheClass = Class.forName("com.vaadin.flow.data.provider.hierarchy.Cache", false, classLoader);

            ROOT_CACHE = HierarchicalDataCommunicator.class.getDeclaredField("rootCache");
            ROOT_CACHE.setAccessible(true);
            GET_FLAT_SIZE = rootCacheClass.getDeclaredMethod("getFlatSize");
            GET_FLAT_SIZE.setAccessible(true);
            GET_CONTEXT_BY_FLAT_INDEX = rootCacheClass.getDeclaredMethod("getContextByFlatIndex", int.class);
            GET_CONTEXT_BY_FLAT_INDEX.setAccessible(true);
            CONTEXT_CACHE = itemContextClass.getDeclaredMethod("cache");
            CONTEXT_CACHE.setAccessible(true);
            CONTEXT_INDEX = itemContextClass.getDeclaredMethod("index");
            CONTEXT_INDEX.setAccessible(true);
            HAS_ITEM = cacheClass.getDeclaredMethod("hasItem", int.class);
            HAS_ITEM.setAccessible(true);
            GET_ITEM = cacheClass.getDeclaredMethod("getItem", int.class);
            GET_ITEM.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T getItemByFlatIndex(HierarchicalDataCommunicator<T> communicator, int flatIndex) {
        try {
            Object rootCache = ROOT_CACHE.get(communicator);
            // the rows of the shadow dom outlive a rebuild of the data, so an index read from one of them can
            // point past what the communicator now holds
            if (rootCache == null || flatIndex < 0 || flatIndex >= (int) GET_FLAT_SIZE.invoke(rootCache)) {
                return null;
            }

            Object context = GET_CONTEXT_BY_FLAT_INDEX.invoke(rootCache, flatIndex);
            if (context == null) {
                return null;
            }

            Object cache = CONTEXT_CACHE.invoke(context);
            int index = (int) CONTEXT_INDEX.invoke(context);
            return (boolean) HAS_ITEM.invoke(cache, index) ? (T) GET_ITEM.invoke(cache, index) : null;
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private HierarchicalDataCommunicatorAccess() {
    }
}
