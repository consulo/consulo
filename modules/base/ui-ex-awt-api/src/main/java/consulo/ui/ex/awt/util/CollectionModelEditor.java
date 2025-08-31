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
package consulo.ui.ex.awt.util;

import consulo.logging.Logger;
import consulo.ui.ex.awt.ElementProducer;
import consulo.util.collection.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class CollectionModelEditor<T, E extends CollectionItemEditor<T>> implements ElementProducer<T> {
    protected static final Logger LOG = Logger.getInstance(CollectionModelEditor.class);

    protected final E itemEditor;
    @Nonnull
    private final Supplier<T> myItemFactory;
    protected final ModelHelper helper = new ModelHelper();

    protected CollectionModelEditor(@Nonnull E itemEditor, @Nonnull Supplier<T> itemFactory) {
        this.itemEditor = itemEditor;
        myItemFactory = itemFactory;
    }

    @Override
    public T createElement() {
        return myItemFactory.get();
    }

    @Override
    public boolean canCreateElement() {
        return true;
    }

    /**
     * Mutable internal list of items (must not be exposed to client)
     */
    @Nonnull
    protected abstract List<T> getItems();

    public void reset(@Nonnull List<T> originalItems) {
        helper.reset(originalItems);
    }

    public final boolean isModified() {
        List<T> items = getItems();
        OrderedSet<T> oldItems = helper.originalItems;
        if (items.size() != oldItems.size()) {
            return true;
        }
        else {
            for (int i = 0, size = items.size(); i < size; i++) {
                if (!items.get(i).equals(oldItems.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void processModifiedItems(@Nonnull BiConsumer<T, T> processor) {
        helper.process(processor);
    }

    @Nonnull
    public final T getMutable(@Nonnull T item) {
        return helper.getMutable(item, -1);
    }

    protected boolean isEditable(@Nonnull T item) {
        return true;
    }

    protected class ModelHelper {
        final OrderedSet<T> originalItems = new OrderedSet<>(HashingStrategy.canonical());

        private final Map<T, T> modifiedToOriginal = Maps.newHashMap(ContainerUtil.<T>identityStrategy());
        private final Map<T, T> originalToModified = Maps.newHashMap(ContainerUtil.<T>identityStrategy());

        public void reset(@Nullable List<T> newOriginalItems) {
            if (newOriginalItems != null) {
                originalItems.clear();
                originalItems.addAll(newOriginalItems);
            }
            modifiedToOriginal.clear();
            originalToModified.clear();
        }

        public void remove(@Nonnull T item) {
            T original = modifiedToOriginal.remove(item);
            if (original != null) {
                originalToModified.remove(original);
            }
        }

        public boolean isMutable(@Nonnull T item) {
            return modifiedToOriginal.containsKey(item) || !originalItems.contains(item);
        }

        @Nonnull
        public T getMutable(@Nonnull T item, int index) {
            if (isMutable(item) || !isEditable(item)) {
                return item;
            }
            else {
                T mutable = originalToModified.get(item);
                if (mutable == null) {
                    mutable = itemEditor.clone(item, false);
                    modifiedToOriginal.put(mutable, item);
                    originalToModified.put(item, mutable);
                    silentlyReplaceItem(item, mutable, index);
                }
                return mutable;
            }
        }

        public boolean hasModifiedItems() {
            return !modifiedToOriginal.isEmpty();
        }

        public void process(@Nonnull BiConsumer<T, T> procedure) {
            modifiedToOriginal.forEach(procedure);
        }
    }

    protected void silentlyReplaceItem(@Nonnull T oldItem, @Nonnull T newItem, int index) {
        // silently replace item
        List<T> items = getItems();
        items.set(index == -1 ? Lists.indexOfIdentity(items, oldItem) : index, newItem);
    }

    protected final boolean areSelectedItemsRemovable(@Nonnull ListSelectionModel selectionMode) {
        int minSelectionIndex = selectionMode.getMinSelectionIndex();
        int maxSelectionIndex = selectionMode.getMaxSelectionIndex();
        if (minSelectionIndex < 0 || maxSelectionIndex < 0) {
            return false;
        }

        List<T> items = getItems();
        for (int i = minSelectionIndex; i <= maxSelectionIndex; i++) {
            if (itemEditor.isRemovable(items.get(i))) {
                return true;
            }
        }
        return false;
    }
}