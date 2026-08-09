/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.internal.UIInternal;
import consulo.ui.model.FlatDataModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public interface ListBox<E> extends ValueComponent<E>, HasSpeedSearch<E>, HasItemSize<E>, HasTransferHandler<E> {
    @SafeVarargs
    static <E> ListBox<E> create(E... elements) {
        return UIInternal.get()._Components_listBox(FlatDataModel.of(Arrays.asList(elements)));
    }

    static <E> ListBox<E> create(Collection<E> elements) {
        return UIInternal.get()._Components_listBox(FlatDataModel.of(elements));
    }

    static <E> ListBox<E> create(FlatDataModel<E> model) {
        return UIInternal.get()._Components_listBox(model);
    }

    FlatDataModel<E> getDataModel();

    void setRender(TextItemRender<E> render);

    void setRender(ComponentItemRender<E> render);

    void setValueByIndex(int index);

    /**
     * Moves the selection by {@code delta} items, stopping at the ends. Selecting nothing yet counts as standing
     * before the first item.
     */
    @RequiredUIAccess
    default void moveSelection(int delta) {
        FlatDataModel<E> model = getDataModel();

        int size = model.getSize();
        if (size == 0) {
            return;
        }

        E value = getValue();
        int index = value == null ? -1 : model.indexOf(value);

        setValueByIndex(Math.max(0, Math.min(size - 1, index + delta)));
    }

    @SuppressWarnings("unchecked")
    default Disposable addDoubleClickListener(ComponentEventListener<ListBox<E>, ListDoubleClickEvent<E>> listener) {
        return addListener((Class)ListDoubleClickEvent.class, listener);
    }

    /**
     * Which items stand between the others rather than being ones of their own. A separator is drawn as a line and
     * cannot be selected.
     */
    default void isSeparator(Predicate<E> predicate) {
    }

    /**
     * Whether the item under the pointer becomes the selected one. What a list of a popup does, where the pointer is
     * what the user is choosing with - selecting is not choosing, so the list only follows the pointer.
     */
    @RequiredUIAccess
    default void setSelectOnHover(boolean selectOnHover) {
    }
}
