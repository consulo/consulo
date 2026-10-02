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
 * A drop-down where several items can be picked - each pick adds an item to the value or takes it away, and the
 * drop-down shows what is picked while it is closed.
 * <p/>
 * The value is the picked items, in the order of the model. Setting a value picks exactly those items; an empty
 * list, like {@code null}, picks nothing. The placeholder is shown while nothing is picked.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface MultiSelectComboBox<E> extends ValueComponent<List<E>>, ComboBoxBase<E>, HasPlaceholder {
    @SafeVarargs
    static <E> MultiSelectComboBox<E> create(E... elements) {
        return UIInternal.get()._Components_multiSelectComboBox(FlatDataModel.of(Arrays.asList(elements)));
    }

    static <E> MultiSelectComboBox<E> create(Collection<E> elements) {
        return UIInternal.get()._Components_multiSelectComboBox(FlatDataModel.of(elements));
    }

    static <E> MultiSelectComboBox<E> create(FlatDataModel<E> model) {
        return UIInternal.get()._Components_multiSelectComboBox(model);
    }

    @Override
    List<E> getValue();
}
