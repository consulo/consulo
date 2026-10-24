// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.list.search;

import consulo.localize.LocalizeValue;
import consulo.review.util.popup.PopupItemPresentation;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class DropDownComponentFactory<T> {
    private final MutableObservableValue<@Nullable T> myState;

    public DropDownComponentFactory(MutableObservableValue<@Nullable T> state) {
        myState = state;
    }

    public AnAction create(LocalizeValue filterName, Function<T, String> valuePresenter, Supplier<ChooserPopup<@Nullable T>> chooseValue) {
        return ReviewListSearchFiltersDropDownComponentFactory.create(
            myState,
            filterName,
            valuePresenter,
            () -> {
                ChooserPopup<@Nullable T> chooser = chooseValue.get();
                chooser.result().thenAccept(myState::set);
                return chooser.popup();
            },
            () -> myState.set(null)
        );
    }

    public AnAction create(LocalizeValue filterName, Supplier<ChooserPopup<@Nullable T>> chooseValue) {
        return create(filterName, Object::toString, chooseValue);
    }

    public AnAction create(LocalizeValue filterName, List<T> items, Runnable onSelect) {
        return create(filterName, items, onSelect, Object::toString);
    }

    public AnAction create(LocalizeValue filterName, List<T> items, Runnable onSelect, Function<T, String> valuePresenter) {
        return create(filterName, items, onSelect, valuePresenter, popupItem -> new PopupItemPresentation.Simple(valuePresenter.apply(popupItem)));
    }

    public AnAction create(LocalizeValue filterName,
                           List<T> items,
                           Runnable onSelect,
                           Function<T, String> valuePresenter,
                           Function<T, PopupItemPresentation> popupItemPresenter) {
        return create(filterName, valuePresenter, () -> {
            ChooserPopup<@Nullable T> chooser = ChooserPopupUtil.createChooserPopup(items, popupItemPresenter, PopupConfig.DEFAULT);
            chooser.result().thenAccept(selectedItem -> {
                if (selectedItem != null) {
                    onSelect.run();
                }
            });
            return chooser;
        });
    }
}
