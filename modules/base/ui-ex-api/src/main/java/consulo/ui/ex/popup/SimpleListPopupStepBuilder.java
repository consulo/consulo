/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui.ex.popup;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.internal.SimpleListPopupStep;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-08-03
 */
public class SimpleListPopupStepBuilder<T> {
    @Nonnull
    public static <V> SimpleListPopupStepBuilder<V> newBuilder(@Nonnull List<? extends V> items) {
        return new SimpleListPopupStepBuilder<>(items);
    }

    private final List<? extends T> myItems;

    private LocalizeValue myTitle = LocalizeValue.empty();

    private Function<T, String> myTextBuilder = Object::toString;

    private Consumer<? super T> myAction = t -> {};

    private T myDefaultValue;

    public SimpleListPopupStepBuilder(List<? extends T> items) {
        myItems = items;
    }

    public SimpleListPopupStepBuilder<T> withTitle(@Nonnull LocalizeValue title) {
        myTitle = title;
        return this;
    }

    public SimpleListPopupStepBuilder<T> withTextBuilder(@Nonnull Function<T, String> function) {
        myTextBuilder = function;
        return this;
    }

    public SimpleListPopupStepBuilder<T> withFinishAction(Consumer<? super T> finishAction) {
       myAction = finishAction;
       return this;
    }

    public SimpleListPopupStepBuilder<T> withDefaultValue(@Nonnull T value) {
        myDefaultValue = value;
        return this;
    }

    public ListPopupStep<T> build() {
        return new SimpleListPopupStep<>(myTitle, myItems, myTextBuilder, myAction, myDefaultValue);
    }
}
