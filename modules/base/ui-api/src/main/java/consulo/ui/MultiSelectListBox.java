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
package consulo.ui;

import consulo.ui.internal.UIInternal;
import consulo.ui.model.FlatDataModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A list where several items can be selected at once - a range, or items picked one by one.
 * <p/>
 * The value is the selected items, in the order of the model. Setting a value selects exactly those items;
 * an empty list, like {@code null}, selects nothing.
 *
 * @author VISTALL
 * @since 2026-10-02
 */
public interface MultiSelectListBox<E> extends ValueComponent<List<E>>, ListBoxBase<E> {
    @SafeVarargs
    static <E> MultiSelectListBox<E> create(E... elements) {
        return UIInternal.get()._Components_multiSelectListBox(FlatDataModel.of(Arrays.asList(elements)));
    }

    static <E> MultiSelectListBox<E> create(Collection<E> elements) {
        return UIInternal.get()._Components_multiSelectListBox(FlatDataModel.of(elements));
    }

    static <E> MultiSelectListBox<E> create(FlatDataModel<E> model) {
        return UIInternal.get()._Components_multiSelectListBox(model);
    }

    @Override
    List<E> getValue();
}
