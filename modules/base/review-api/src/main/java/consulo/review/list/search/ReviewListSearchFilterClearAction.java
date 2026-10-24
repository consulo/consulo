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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * Resets a filter of the review list; shown while the filter has a value.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
final class ReviewListSearchFilterClearAction extends AnAction implements DumbAware, AnActionWithSyncUpdate {
    private final ObservableValue<? extends @Nullable Object> myFilterState;
    private final Runnable myClearFilterValue;

    ReviewListSearchFilterClearAction(ObservableValue<? extends @Nullable Object> filterState, Runnable clearFilterValue) {
        super(ReviewLocalize.reviewListFilterClear(), ReviewLocalize.reviewListFilterClear(), PlatformIconGroup.actionsClose());
        myFilterState = filterState;
        myClearFilterValue = clearFilterValue;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(myFilterState.get() != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        myClearFilterValue.run();
    }
}
