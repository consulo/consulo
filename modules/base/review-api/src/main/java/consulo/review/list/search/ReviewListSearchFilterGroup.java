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

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A review list filter whose values come from a chooser: its name, the chosen value and the action showing the chooser.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
final class ReviewListSearchFilterGroup<T> extends ActionGroup implements DumbAware {
    private final ObservableValue<@Nullable T> myFilterState;
    private final LocalizeValue myFilterName;
    private final Function<T, String> myValuePresenter;
    private final Runnable myClearFilterValue;
    private final AnAction myChooseAction;

    ReviewListSearchFilterGroup(ObservableValue<@Nullable T> filterState,
                                LocalizeValue filterName,
                                Function<T, String> valuePresenter,
                                Supplier<JBPopup> chooseFilterValue,
                                Runnable clearFilterValue) {
        myFilterState = filterState;
        myFilterName = filterName;
        myValuePresenter = valuePresenter;
        myClearFilterValue = clearFilterValue;
        myChooseAction = new ReviewListSearchFilterChooseAction(chooseFilterValue);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> children = new ArrayList<>();
        children.add(AnSeparator.create(myFilterName));
        T value = myFilterState.get();
        if (value != null) {
            children.add(new ReviewListSearchFilterValueAction<>(myFilterState, value, LocalizeValue.of(myValuePresenter.apply(value)),
                it -> {
                }, myClearFilterValue));
        }
        children.add(myChooseAction);
        return children.toArray(AnAction[]::new);
    }
}
