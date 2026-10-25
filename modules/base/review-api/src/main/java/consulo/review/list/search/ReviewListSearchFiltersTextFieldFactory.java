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

import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.event.details.KeyCode;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

final class ReviewListSearchFiltersTextFieldFactory {
    private static final String SHOW_SEARCH_HISTORY = "ShowSearchHistory";

    private ReviewListSearchFiltersTextFieldFactory() {
    }

    @RequiredUIAccess
    static TextBox create(Disposable parentDisposable,
                          ObservableValue<@Nullable String> searchTextFlow,
                          @Nullable Consumer<String> setSearchText,
                          Consumer<@Nullable String> submitSearchText,
                          Consumer<TextBox> chooseFromHistory) {
        TextBox searchField = TextBox.create();
        searchField.addKeyPressedListener(event -> {
            KeyCode keyCode = event.getInputDetails().getKeyCode();
            if (keyCode != null && keyCode.key() == KeyCode.ENTER.key()) {
                submitSearchText.accept(StringUtil.nullize(searchField.getValue()));
            }
        });

        ReviewUIUtil.bind(parentDisposable, searchTextFlow, text -> {
            String value = StringUtil.notNullize(StringUtil.nullize(text));
            if (!value.equals(StringUtil.notNullize(searchField.getValue()))) {
                searchField.setValue(value);
            }
        });
        if (setSearchText != null) {
            searchField.addValueListener(event -> setSearchText.accept(StringUtil.notNullize(event.getValue())));
        }

        AnAction showHistoryAction = new ShowHistoryAction(() -> chooseFromHistory.accept(searchField));
        showHistoryAction.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(SHOW_SEARCH_HISTORY), searchField);

        ActionToolbar historyToolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("ReviewListSearchHistory", ActionGroup.of(showHistoryAction), ActionToolbar.Style.INPLACE);
        historyToolbar.setTargetUIComponent(searchField);
        searchField.setPrefixComponent(historyToolbar.getUIComponent());

        return searchField;
    }

    private static final class ShowHistoryAction extends AnAction implements DumbAware {
        private final Runnable myShowHistory;

        private ShowHistoryAction(Runnable showHistory) {
            super(
                ReviewLocalize.reviewListSearchHistoryTooltip(
                    KeymapUtil.getPreferredShortcutText(KeymapUtil.getActiveKeymapShortcuts(SHOW_SEARCH_HISTORY).getShortcuts())
                ),
                ReviewLocalize.reviewListSearchHistoryTooltip(
                    KeymapUtil.getPreferredShortcutText(KeymapUtil.getActiveKeymapShortcuts(SHOW_SEARCH_HISTORY).getShortcuts())
                ),
                PlatformIconGroup.actionsSearchwithhistory()
            );
            myShowHistory = showHistory;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myShowHistory.run();
        }
    }
}
