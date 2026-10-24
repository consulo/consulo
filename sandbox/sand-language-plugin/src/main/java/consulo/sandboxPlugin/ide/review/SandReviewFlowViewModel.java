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
package consulo.sandboxPlugin.ide.review;

import consulo.localize.LocalizeValue;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.data.ReviewState;
import consulo.review.details.model.CodeReviewFlowViewModel;
import consulo.review.util.Either;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The reviewers of a sand review and what can be done with it; every change takes a moment, during which the review is busy.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewFlowViewModel implements CodeReviewFlowViewModel<SandReviewUser> {
    private static final long BUSY_MILLISECONDS = 600;

    private final ScheduledExecutorService myScheduler;
    private final MutableObservableValue<ReviewRequestState> myRequestState;
    private final MutableObservableValue<@Nullable Boolean> myHasConflicts;
    private final MutableObservableValue<Map<SandReviewUser, ReviewState>> myReviewerReviews;
    private final MutableObservableValue<List<SandReviewUser>> myRequestedReviewers = MutableObservableValue.of(List.of());
    private final MutableObservableValue<Boolean> myBusy = MutableObservableValue.of(false);
    private final MutableObservableValue<Boolean> myHasUnresolvedConversations;
    private final ObservableValue<ReviewState> myReviewState;
    private final ObservableValue<Either<LocalizeValue, Runnable>> myResolveConflictsAction;

    public SandReviewFlowViewModel(ScheduledExecutorService scheduler,
                                   MutableObservableValue<ReviewRequestState> requestState,
                                   MutableObservableValue<@Nullable Boolean> hasConflicts,
                                   Map<SandReviewUser, ReviewState> reviewerReviews,
                                   boolean hasUnresolvedConversations) {
        myScheduler = scheduler;
        myRequestState = requestState;
        myHasConflicts = hasConflicts;
        myReviewerReviews = MutableObservableValue.of(Map.copyOf(reviewerReviews));
        myHasUnresolvedConversations = MutableObservableValue.of(hasUnresolvedConversations);
        myReviewState = myReviewerReviews.map(SandReviewFlowViewModel::calcReviewState);
        myResolveConflictsAction = myRequestState.map(state -> state == ReviewRequestState.DRAFT
            ? Either.<LocalizeValue, Runnable>left(LocalizeValue.localizeTODO("Conflicts of a draft are resolved locally"))
            : Either.<LocalizeValue, Runnable>right(this::resolveConflicts));
    }

    @Override
    public ObservableValue<Map<SandReviewUser, ReviewState>> getReviewerReviews() {
        return myReviewerReviews;
    }

    public ObservableValue<ReviewState> getReviewState() {
        return myReviewState;
    }

    public ObservableValue<List<SandReviewUser>> getRequestedReviewers() {
        return myRequestedReviewers;
    }

    public ObservableValue<Boolean> isBusy() {
        return myBusy;
    }

    public ObservableValue<Boolean> getHasUnresolvedConversations() {
        return myHasUnresolvedConversations;
    }

    public ObservableValue<Integer> getRequiredApprovingReviewsCount() {
        return myReviewState.map(state -> state == ReviewState.ACCEPTED ? 0 : 1);
    }

    public ObservableValue<Boolean> isDraft() {
        return myRequestState.map(state -> state == ReviewRequestState.DRAFT);
    }

    public ObservableValue<Either<LocalizeValue, Runnable>> getResolveConflictsAction() {
        return myResolveConflictsAction;
    }

    public void requestReview() {
        runBusy(() -> {
            setReview(SandReviewService.REVIEWER, ReviewState.NEED_REVIEW);
            myRequestedReviewers.set(List.of(SandReviewService.REVIEWER));
        });
    }

    public void reRequestReview() {
        requestReview();
    }

    public void setMyselfAsReviewer() {
        runBusy(() -> setReview(SandReviewService.CURRENT_USER, ReviewState.NEED_REVIEW));
    }

    public void removeReviewer(SandReviewUser reviewer) {
        runBusy(() -> {
            Map<SandReviewUser, ReviewState> reviews = new LinkedHashMap<>(myReviewerReviews.get());
            reviews.remove(reviewer);
            myReviewerReviews.set(reviews);
            List<SandReviewUser> requested = new ArrayList<>(myRequestedReviewers.get());
            requested.remove(reviewer);
            myRequestedReviewers.set(requested);
        });
    }

    public void submitReview(ReviewState state, Runnable onDone) {
        runBusy(() -> {
            setReview(SandReviewService.CURRENT_USER, state);
            if (state == ReviewState.ACCEPTED) {
                myHasUnresolvedConversations.set(false);
            }
            onDone.run();
        });
    }

    public void merge() {
        runBusy(() -> myRequestState.set(ReviewRequestState.MERGED));
    }

    public void close() {
        runBusy(() -> myRequestState.set(ReviewRequestState.CLOSED));
    }

    public void reopen() {
        runBusy(() -> myRequestState.set(ReviewRequestState.OPENED));
    }

    public void postReview() {
        runBusy(() -> myRequestState.set(ReviewRequestState.OPENED));
    }

    private void resolveConflicts() {
        runBusy(() -> myHasConflicts.set(false));
    }

    private void setReview(SandReviewUser reviewer, ReviewState state) {
        Map<SandReviewUser, ReviewState> reviews = new LinkedHashMap<>(myReviewerReviews.get());
        reviews.put(reviewer, state);
        myReviewerReviews.set(reviews);
    }

    private void runBusy(Runnable change) {
        if (!myBusy.compareAndSet(false, true)) {
            return;
        }
        myScheduler.schedule(() -> {
            try {
                change.run();
            }
            finally {
                myBusy.set(false);
            }
        }, BUSY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    private static ReviewState calcReviewState(Map<SandReviewUser, ReviewState> reviews) {
        if (reviews.containsValue(ReviewState.WAIT_FOR_UPDATES)) {
            return ReviewState.WAIT_FOR_UPDATES;
        }
        if (!reviews.isEmpty() && !reviews.containsValue(ReviewState.NEED_REVIEW)) {
            return ReviewState.ACCEPTED;
        }
        return ReviewState.NEED_REVIEW;
    }
}
