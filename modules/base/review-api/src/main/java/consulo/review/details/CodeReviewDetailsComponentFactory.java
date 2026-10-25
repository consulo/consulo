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
package consulo.review.details;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.details.model.CodeReviewDetailsViewModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The parts of a review details tab, and the whole tab built of them. A provider showing more than the platform knows
 * composes its own tab from the parts.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewDetailsComponentFactory {
    /**
     * The title section, the description with the link to the timeline, and the status checks.
     */
    @RequiredUIAccess
    Component create(Disposable parentDisposable, CodeReviewDetailsTabViewModel tabVm, @Nullable @RequiredUIAccess Runnable showTimelineAction);

    /**
     * The review title, its number linking to the review, and the state tag of a review that is not open.
     */
    @RequiredUIAccess
    Component createTitle(Disposable parentDisposable, CodeReviewDetailsViewModel detailsVm);

    /**
     * The first lines of the description, and the link to the timeline when {@code showTimelineAction} is given.
     */
    @RequiredUIAccess
    Component createDescription(Disposable parentDisposable,
                                CodeReviewDetailsViewModel detailsVm,
                                @Nullable @RequiredUIAccess Runnable showTimelineAction);
}
