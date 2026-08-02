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
package com.vaadin.flow.data.provider.hierarchy;

import org.jspecify.annotations.Nullable;

/**
 * A row of a tree grid answers the flat index it is drawn at, and that index is what the communicator keeps
 * its cache by - but {@link HierarchicalDataCommunicator} publishes nothing that takes one.
 * {@code DataCommunicator#getItem(int)} is the flat lookup of the non hierarchical case and is not overridden,
 * so it walks the wrong cache. The cache that does answer is package private, hence this class living in the
 * vaadin package: it reads the item and nothing else.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public final class HierarchicalDataCommunicatorAccess {
    public static <T> @Nullable T getItemByFlatIndex(HierarchicalDataCommunicator<T> communicator, int flatIndex) {
        RootCache<T> rootCache = communicator.rootCache;
        // the rows of the shadow dom outlive a rebuild of the data, so an index read from one of them can
        // point past what the communicator now holds
        if (rootCache == null || flatIndex < 0 || flatIndex >= rootCache.getFlatSize()) {
            return null;
        }

        RootCache.ItemContext<T> context = rootCache.getContextByFlatIndex(flatIndex);
        if (context == null) {
            return null;
        }

        Cache<T> cache = context.cache();
        int index = context.index();
        return cache.hasItem(index) ? cache.getItem(index) : null;
    }

    private HierarchicalDataCommunicatorAccess() {
    }
}
