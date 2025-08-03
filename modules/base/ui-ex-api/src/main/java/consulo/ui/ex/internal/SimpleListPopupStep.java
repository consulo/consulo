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
package consulo.ui.ex.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-08-03
 */
public class SimpleListPopupStep<T> extends BaseListPopupStep<T> {
    private final Function<T, String> myTextBuilder;
    private final Consumer<? super T> myFinishAction;

    public SimpleListPopupStep(LocalizeValue title,
                               List<? extends T> items,
                               Function<T, String> textBuilder,
                               Consumer<? super T> finishAction) {
        super(title.get(), items);
        myTextBuilder = textBuilder;
        myFinishAction = finishAction;
    }

    @Override
    public PopupStep onChosen(T selectedValue, boolean finalChoice) {
        myFinishAction.accept(selectedValue);
        return FINAL_CHOICE;
    }

    @Nonnull
    @Override
    public String getTextFor(T value) {
        return myTextBuilder.apply(value);
    }
}
