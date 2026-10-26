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
package consulo.review.details.model;

import consulo.dataContext.DataSink;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.toolwindow.ReviewTabViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A review tool window tab showing one review.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewDetailsTabViewModel extends ReviewTabViewModel {
    CodeReviewDetailsViewModel getDetailsVm();

    @Nullable
    default CodeReviewStatusViewModel getStatusVm() {
        return null;
    }

    @Nullable
    default CodeReviewTimelineViewModel getTimelineVm() {
        return null;
    }

    /**
     * The icon of the editor tab showing the timeline, such as the state of the review, or {@code null} for the default
     * one. Read again whenever the tab is presented.
     */
    default @Nullable Image getIcon() {
        return null;
    }

    /**
     * Fills the data context of the timeline - the actions of its {@linkplain #getPopupActionGroup() context menu}
     * read the review from it.
     */
    @RequiredUIAccess
    default void uiDataSnapshot(DataSink sink) {
    }

    /**
     * The context menu of the timeline, or {@code null} for none.
     */
    default @Nullable ActionGroup getPopupActionGroup() {
        return null;
    }
}
