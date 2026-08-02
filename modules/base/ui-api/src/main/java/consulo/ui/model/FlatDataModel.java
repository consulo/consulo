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
package consulo.ui.model;

import consulo.disposer.Disposable;
import consulo.ui.internal.UIInternal;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Model of a flat, indexed sequence of items, shared by every flat collection component -
 * {@code Table}, {@code ListBox} and {@code ComboBox}. {@code Tree} keeps its own hierarchical
 * {@code TreeModel}.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface FlatDataModel<E> extends Iterable<E> {
    static <T> MutableFlatDataModel<T> of(Collection<? extends T> items) {
        return UIInternal.get()._FlatDataModel_create(items);
    }

    int getSize();

    E get(int index);

    int indexOf(E item);

    /**
     * Stable identity of an item, used to map an item to its row across refetches. Override when
     * items are replaced by equal-but-not-identical instances.
     */
    default Object getId(E item) {
        return item;
    }

    Disposable addListener(Consumer<FlatDataModelEvent> listener);
}
