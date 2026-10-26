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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A value of a review list filter: selected while the filter holds it, choosing it otherwise, and resetting the filter
 * when chosen again.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
final class ReviewListSearchFilterValueAction<T> extends ToggleAction implements DumbAware {
    private final ObservableValue<@Nullable T> myFilterState;
    private final T myValue;
    private final Consumer<T> myChooseValue;
    private final Runnable myClearValue;

    ReviewListSearchFilterValueAction(ObservableValue<@Nullable T> filterState,
                                      T value,
                                      LocalizeValue text,
                                      Consumer<T> chooseValue,
                                      Runnable clearValue) {
        super(text);
        myFilterState = filterState;
        myValue = value;
        myChooseValue = chooseValue;
        myClearValue = clearValue;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return Objects.equals(myFilterState.get(), myValue);
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        if (state) {
            myChooseValue.accept(myValue);
        }
        else {
            myClearValue.run();
        }
    }
}
