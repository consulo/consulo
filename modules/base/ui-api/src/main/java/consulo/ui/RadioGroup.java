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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A set of options of which one is chosen, and the value of that choice.
 * <p/>
 * The group is what carries the answer - it is read and written as the type the caller already thinks in, so a
 * setting of three states is a group of that type rather than three booleans a screen has to tell apart. Only the
 * group can make a {@link RadioButton}, because a button on its own has nothing to be exclusive with and no way to
 * say what choosing it means.
 * <p/>
 * A group is not a component and is never placed. It hands back buttons and whoever asked for them decides where
 * they go, so options can sit in a column, in a row, or spread through a form with the fields each of them
 * enables. The {@code fill} methods are only shorthand for the usual columns and rows.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public interface RadioGroup<V> {
    static <V> RadioGroup<V> create() {
        return UIInternal.get()._Components_radioGroup();
    }

    /**
     * A new option of this group, unselected, which the caller places wherever it belongs.
     */
    @RequiredUIAccess
    RadioButton newButton(LocalizeValue text, V value);

    /**
     * The value of the chosen option, or null while nothing is chosen.
     */
    @Nullable V getValue();

    V getValueOrError();

    /**
     * Chooses the option carrying this value, and unchooses whichever was chosen before. A value no option
     * carries leaves the group with nothing chosen.
     */
    @RequiredUIAccess
    void setValue(@Nullable V value);

    @RequiredUIAccess
    void setValue(@Nullable V value, boolean fireListeners);

    Disposable addValueListener(Consumer<V> listener);

    @RequiredUIAccess
    default void fillToLayout(VerticalLayout layout, Collection<? extends V> values, Function<V, LocalizeValue> presentation) {
        for (V value : values) {
            layout.add(newButton(presentation.apply(value), value));
        }
    }

    @RequiredUIAccess
    default void fillToLayout(HorizontalLayout layout, Collection<? extends V> values, Function<V, LocalizeValue> presentation) {
        for (V value : values) {
            layout.add(newButton(presentation.apply(value), value));
        }
    }

    @RequiredUIAccess
    default VerticalLayout fillVertical(Collection<? extends V> values, Function<V, LocalizeValue> presentation) {
        VerticalLayout layout = VerticalLayout.create();
        fillToLayout(layout, values, presentation);
        return layout;
    }

    @RequiredUIAccess
    default HorizontalLayout fillHorizontal(Collection<? extends V> values, Function<V, LocalizeValue> presentation) {
        HorizontalLayout layout = HorizontalLayout.create();
        fillToLayout(layout, values, presentation);
        return layout;
    }

    @RequiredUIAccess
    default VerticalLayout fillByEnum(Class<? extends V> clazz, Function<V, LocalizeValue> presentation) {
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException("Accepts enums only");
        }

        return fillVertical(List.of(clazz.getEnumConstants()), presentation);
    }
}
