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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.model.CodeReviewDetailsViewModel;
import consulo.review.details.model.CodeReviewStatusViewModel;
import consulo.review.icon.CIBuildStatusIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.list.CommentsCounter;
import consulo.review.list.ReviewListItemPresentation;
import consulo.review.list.ReviewListItemStatus;
import consulo.review.list.SimpleReviewListItemPresentation;
import consulo.review.list.SimpleUserPresentation;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewViewModel implements CodeReviewDetailsViewModel, CodeReviewStatusViewModel {
    private final String myNumber;
    private final SandReviewUser myAuthor;
    private final Date myCreatedDate;
    private final MutableObservableValue<String> myTitle;
    private final MutableObservableValue<String> myDescription;
    private final MutableObservableValue<ReviewRequestState> myState;
    private final MutableObservableValue<@Nullable Boolean> myHasConflicts;
    private final MutableObservableValue<List<CodeReviewCIJob>> myCiJobs;
    private final List<SandReviewThreadViewModel> myThreads;
    private final List<Consumer<List<CodeReviewCIJob>>> myShowJobsListeners = new CopyOnWriteArrayList<>();

    public SandReviewViewModel(String number,
                               String title,
                               String description,
                               ReviewRequestState state,
                               SandReviewUser author,
                               Date createdDate,
                               @Nullable Boolean hasConflicts,
                               List<CodeReviewCIJob> ciJobs,
                               List<SandReviewThreadViewModel> threads) {
        myNumber = number;
        myAuthor = author;
        myCreatedDate = createdDate;
        myTitle = MutableObservableValue.of(title);
        myDescription = MutableObservableValue.of(description);
        myState = MutableObservableValue.of(state);
        myHasConflicts = MutableObservableValue.of(hasConflicts);
        myCiJobs = MutableObservableValue.of(ciJobs);
        myThreads = threads;
    }

    @Override
    public String getNumber() {
        return myNumber;
    }

    @Override
    public String getUrl() {
        return "https://sand.consulo.io/review/" + myNumber;
    }

    @Override
    public ObservableValue<String> getTitle() {
        return myTitle;
    }

    @Override
    public ObservableValue<String> getDescription() {
        return myDescription;
    }

    @Override
    public ObservableValue<ReviewRequestState> getReviewRequestState() {
        return myState;
    }

    public void setReviewRequestState(ReviewRequestState state) {
        myState.set(state);
    }

    @Override
    public ObservableValue<@Nullable Boolean> getHasConflicts() {
        return myHasConflicts;
    }

    @Override
    public ObservableValue<List<CodeReviewCIJob>> getCiJobs() {
        return myCiJobs;
    }

    @Override
    public void addShowJobsDetailsRequestListener(Disposable parentDisposable, Consumer<List<CodeReviewCIJob>> listener) {
        myShowJobsListeners.add(listener);
        Disposer.register(parentDisposable, () -> myShowJobsListeners.remove(listener));
    }

    @Override
    public void showJobsDetails() {
        List<CodeReviewCIJob> jobs = myCiJobs.get();
        for (Consumer<List<CodeReviewCIJob>> listener : myShowJobsListeners) {
            listener.accept(jobs);
        }
    }

    public SandReviewUser getAuthor() {
        return myAuthor;
    }

    public List<SandReviewThreadViewModel> getThreads() {
        return myThreads;
    }

    public ReviewListItemPresentation toPresentation() {
        int comments = 0;
        for (SandReviewThreadViewModel thread : myThreads) {
            comments += thread.getComments().get().size();
        }
        return new SimpleReviewListItemPresentation(
            myTitle.get(),
            "#" + myNumber,
            myCreatedDate,
            new SimpleUserPresentation(myAuthor.username(), myAuthor.fullName(), PlatformIconGroup.actionsLoginavatar()),
            null,
            Boolean.TRUE.equals(myHasConflicts.get())
                ? new ReviewListItemStatus(PlatformIconGroup.generalError(), LocalizeValue.localizeTODO("Has conflicts"))
                : null,
            buildStatus(myCiJobs.get()),
            myState.get().name(),
            null,
            null,
            new CommentsCounter(comments, LocalizeValue.localizeTODO(comments + " comments")),
            null
        );
    }

    public static @Nullable ReviewListItemStatus buildStatus(List<CodeReviewCIJob> jobs) {
        if (jobs.isEmpty()) {
            return null;
        }
        boolean pending = false;
        for (CodeReviewCIJob job : jobs) {
            if (job.status() == CodeReviewCIJobState.FAILED) {
                return new ReviewListItemStatus(CIBuildStatusIcons.failed(), LocalizeValue.localizeTODO("Checks failed"));
            }
            pending |= job.status() == CodeReviewCIJobState.PENDING;
        }
        return pending
            ? new ReviewListItemStatus(CIBuildStatusIcons.inProgress(), LocalizeValue.localizeTODO("Checks are running"))
            : new ReviewListItemStatus(CIBuildStatusIcons.success(), LocalizeValue.localizeTODO("Checks passed"));
    }
}
