// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.data.ReviewState;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;

/**
 * The buttons of the review details. A button follows the text, the description and the enabled state of the template presentation
 * of its action, and performs the action when clicked.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewDetailsActionsComponentFactory {
    Space BUTTONS_GAP = Space.LARGE;

    @RequiredUIAccess
    <Reviewer> Component createRequestReviewButton(Disposable parentDisposable,
                                                   ObservableValue<ReviewState> reviewState,
                                                   ObservableValue<List<Reviewer>> requestedReviewers,
                                                   AnAction requestReviewAction);

    @RequiredUIAccess
    <Reviewer> Component createReRequestReviewButton(Disposable parentDisposable,
                                                     ObservableValue<ReviewState> reviewState,
                                                     ObservableValue<List<Reviewer>> requestedReviewers,
                                                     AnAction reRequestReviewAction);

    @RequiredUIAccess
    Component createActionsComponent(Disposable parentDisposable,
                                     ObservableValue<ReviewRequestState> reviewRequestState,
                                     Component openedStatePanel,
                                     Component mergedStatePanel,
                                     Component closedStatePanel,
                                     Component draftedStatePanel);

    @RequiredUIAccess
    Component createActionsForMergedReview();

    @RequiredUIAccess
    Component createActionsForClosedReview(Disposable parentDisposable, AnAction reopenReviewAction);

    @RequiredUIAccess
    Component createActionsForDraftReview(Disposable parentDisposable, AnAction postReviewAction);

    @RequiredUIAccess
    Component createMoreButton(ActionGroup actionGroup);
}
