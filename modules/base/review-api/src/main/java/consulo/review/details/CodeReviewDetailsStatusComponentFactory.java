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
import consulo.localize.LocalizeValue;
import consulo.review.details.data.ReviewState;
import consulo.review.details.model.CodeReviewStatusViewModel;
import consulo.review.util.Either;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewDetailsStatusComponentFactory {
    @RequiredUIAccess
    Component createErrorComponent(LocalizeValue message, @Nullable AnAction action);

    @RequiredUIAccess
    default Component createErrorComponent(LocalizeValue message) {
        return createErrorComponent(message, null);
    }

    /**
     * @param resolveAction If a {@link Runnable} is emitted, there's some action to execute on click.
     *                      If a text is emitted, it's the tooltip text to tell why no action is available,
     *                      an empty text tells there is no action at all.
     */
    @RequiredUIAccess
    Component createConflictsComponent(Disposable parentDisposable,
                                       ObservableValue<@Nullable Boolean> hasConflicts,
                                       ObservableValue<Either<LocalizeValue, Runnable>> resolveAction,
                                       ObservableValue<Boolean> isBusy);

    @RequiredUIAccess
    default Component createConflictsComponent(Disposable parentDisposable, ObservableValue<@Nullable Boolean> hasConflicts) {
        return createConflictsComponent(parentDisposable,
            hasConflicts,
            ObservableValue.of(Either.left(LocalizeValue.empty())),
            ObservableValue.of(false));
    }

    @RequiredUIAccess
    Component createMergeClarificationComponent(Disposable parentDisposable, ObservableValue<String> clarification);

    @RequiredUIAccess
    <T> Component createNeedReviewerComponent(Disposable parentDisposable, ObservableValue<Map<T, ReviewState>> reviewersReview);

    @RequiredUIAccess
    Component createRequiredReviewsComponent(Disposable parentDisposable,
                                             ObservableValue<Integer> requiredApprovingReviewsCount,
                                             ObservableValue<Boolean> isDraft);

    @RequiredUIAccess
    Component createRequiredResolveConversationsComponent(Disposable parentDisposable, ObservableValue<Boolean> hasUnresolvedConversations);

    @RequiredUIAccess
    Component createRestrictionComponent(Disposable parentDisposable, ObservableValue<Boolean> isRestricted, ObservableValue<Boolean> isDraft);

    /**
     * The summary of the CI checks, and the link to the list of checks; hidden while there are none.
     */
    @RequiredUIAccess
    Component createCiComponent(Disposable parentDisposable, CodeReviewStatusViewModel statusVm);

    @RequiredUIAccess
    <Reviewer, ReviewStateType> Component createReviewersReviewStateComponent(
        Disposable parentDisposable,
        ObservableValue<Map<Reviewer, ReviewStateType>> reviewersReview,
        BiFunction<Reviewer, ReviewStateType, LocalizeValue> statusText,
        BiFunction<Reviewer, ReviewStateType, Component> avatar,
        BiFunction<Reviewer, ReviewStateType, @Nullable Image> statusIcon,
        BiFunction<Reviewer, ReviewStateType, @Nullable ActionGroup> actionGroup
    );

    @RequiredUIAccess
    default <Reviewer> Component createReviewersReviewStateComponent(
        Disposable parentDisposable,
        ObservableValue<Map<Reviewer, ReviewState>> reviewersReview,
        Function<Reviewer, String> reviewerName,
        Function<Reviewer, @Nullable Image> reviewerAvatar,
        BiFunction<Reviewer, ReviewState, @Nullable ActionGroup> actionGroup
    ) {
        return createReviewersReviewStateComponent(
            parentDisposable,
            reviewersReview,
            (reviewer, reviewState) -> ReviewDetailsUIUtil.getReviewStateText(reviewState, reviewerName.apply(reviewer)),
            (reviewer, reviewState) -> {
                Avatar avatar = Avatar.create(LocalizeValue.of(reviewerName.apply(reviewer)), reviewerAvatar.apply(reviewer));
                avatar.addStyle(AvatarStyle.X_SMALL);
                avatar.addStyle(ReviewDetailsUIUtil.getReviewStateAvatarStyle(reviewState));
                return avatar;
            },
            (reviewer, reviewState) -> ReviewDetailsUIUtil.getReviewStateIcon(reviewState),
            actionGroup
        );
    }

    @RequiredUIAccess
    default <Reviewer> Component createReviewersReviewStateComponent(
        Disposable parentDisposable,
        ObservableValue<Map<Reviewer, ReviewState>> reviewersReview,
        Function<Reviewer, String> reviewerName,
        Function<Reviewer, @Nullable Image> reviewerAvatar
    ) {
        return createReviewersReviewStateComponent(parentDisposable, reviewersReview, reviewerName, reviewerAvatar, (reviewer, reviewState) -> null);
    }
}
