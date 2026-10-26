// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Filters of the review list are action groups of the filter popup: the filter name as a separator, then its values as
 * toggles - or the chosen value and the action showing {@code chooseFilterValue} when the values come from a chooser.
 */
public final class ReviewListSearchFiltersDropDownComponentFactory {
    private ReviewListSearchFiltersDropDownComponentFactory() {
    }

    public static <T> AnAction create(ObservableValue<@Nullable T> filterState,
                                      LocalizeValue filterName,
                                      Function<T, String> valuePresenter,
                                      Supplier<JBPopup> chooseFilterValue,
                                      Runnable clearFilterValue) {
        return new ReviewListSearchFilterGroup<>(filterState, filterName, valuePresenter, chooseFilterValue, clearFilterValue);
    }

    public static <T> AnAction create(ObservableValue<@Nullable T> filterState,
                                      LocalizeValue filterName,
                                      List<T> items,
                                      Runnable onSelect,
                                      Function<T, String> valuePresenter,
                                      Function<T, PopupItemPresentation> popupItemPresenter,
                                      Consumer<@Nullable T> chooseFilterValue,
                                      Runnable clearFilterValue) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(AnSeparator.create(filterName));
        for (T item : items) {
            PopupItemPresentation presentation = popupItemPresenter.apply(item);
            String fullText = presentation.getFullText();
            group.add(new ReviewListSearchFilterValueAction<>(filterState,
                item,
                LocalizeValue.of(fullText != null ? fullText : presentation.getShortText()),
                value -> {
                    onSelect.run();
                    chooseFilterValue.accept(value);
                },
                clearFilterValue));
        }
        return group;
    }

    public static <T> AnAction create(ObservableValue<@Nullable T> filterState,
                                      LocalizeValue filterName,
                                      List<T> items,
                                      Runnable onSelect,
                                      Function<T, String> valuePresenter,
                                      Consumer<@Nullable T> chooseFilterValue,
                                      Runnable clearFilterValue) {
        return create(
            filterState,
            filterName,
            items,
            onSelect,
            valuePresenter,
            popupItem -> new PopupItemPresentation.Simple(valuePresenter.apply(popupItem)),
            chooseFilterValue,
            clearFilterValue
        );
    }
}
