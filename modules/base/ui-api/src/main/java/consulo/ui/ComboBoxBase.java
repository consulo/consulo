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

import consulo.localize.LocalizeValue;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * What a drop-down of items is, whether one item or several can be picked in it - see {@link ComboBox} and
 * {@link MultiSelectComboBox}.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ComboBoxBase<E> extends HasSpeedSearch<E>, HasItemSize<E>, HasComponentStyle<ComboBoxStyle> {
    FlatDataModel<E> getDataModel();

    void setRender(TextItemRender<E> render);

    void setRender(ComponentItemRender<E> render);

    default void setTextRenderer(Function<@Nullable E, LocalizeValue> localizeValueFunction) {
        setRender((presentation, item) -> presentation.append(localizeValueFunction.apply(item.getValue())));
    }
}
