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
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A filter of the review list: its name while nothing is chosen, the chosen value otherwise, and the chooser of the
 * value as its popup.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
final class ReviewListSearchFilterComboBoxAction<T> extends ComboBoxAction implements DumbAware {
    private final ObservableValue<@Nullable T> myFilterState;
    private final LocalizeValue myFilterName;
    private final Function<T, String> myValuePresenter;
    private final Supplier<JBPopup> myChooser;

    ReviewListSearchFilterComboBoxAction(ObservableValue<@Nullable T> filterState,
                                         LocalizeValue filterName,
                                         Function<T, String> valuePresenter,
                                         Supplier<JBPopup> chooser) {
        myFilterState = filterState;
        myFilterName = filterName;
        myValuePresenter = valuePresenter;
        myChooser = chooser;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        T value = myFilterState.get();
        presentation.setText(value == null ? myFilterName : LocalizeValue.of(myValuePresenter.apply(value)));
        presentation.setDescription(myFilterName);
    }

    @RequiredUIAccess
    @Override
    public JBPopup createPopup(DataContext context, @Nullable Runnable onDispose) {
        JBPopup popup = myChooser.get();
        if (onDispose != null) {
            popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                    onDispose.run();
                }
            });
        }
        return popup;
    }

    @Override
    protected ActionGroup createPopupActionGroup(DataContext context) {
        return ActionGroup.EMPTY_GROUP;
    }
}
