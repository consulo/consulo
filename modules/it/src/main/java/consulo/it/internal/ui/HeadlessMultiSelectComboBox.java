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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.MultiSelectComboBox;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Dummy-but-creatable headless {@link MultiSelectComboBox}: the value is the given items that are in the model, in model order.
 *
 * @author VISTALL
 */
public class HeadlessMultiSelectComboBox<E> extends HeadlessComboBoxBase<E, List<E>> implements MultiSelectComboBox<E> {
    public HeadlessMultiSelectComboBox(FlatDataModel<E> model) {
        super(model, List.of());
    }

    @Override
    public List<E> getValue() {
        List<E> value = super.getValue();
        return value == null ? List.of() : value;
    }

    @Override
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        super.setValue(HeadlessModelOrder.retain(getDataModel(), value), fireListeners);
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
    }

    @Override
    public void setSummaryRenderer(Function<List<E>, LocalizeValue> renderer) {
    }
}
