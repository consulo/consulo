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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.data.ReviewState;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An in-memory review service of a project - the reviews, threads and replies the review UI is tried against.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SandReviewService {
    public static final SandReviewUser CURRENT_USER = new SandReviewUser("vistall", "Valery Semenchuk");
    public static final SandReviewUser REVIEWER = new SandReviewUser("sand-bot", "Sand Reviewer");

    public static final String DEMO_FILE_NAME = "ReviewDemo.txt";

    private final Project myProject;
    private final Disposable myDisposable;
    private final ScheduledExecutorService myScheduler;
    private final String mySessionId = UUID.randomUUID().toString();
    private final Map<String, SandReviewDiffProcessorViewModel> myDiffVms = new HashMap<>();
    private final List<SandReviewViewModel> myReviews;
    private final SandReviewToolwindowViewModel myToolwindowVm;
    private final MutableObservableValue<Integer> myThreadsModificationCount = MutableObservableValue.of(0);
    private final SandReviewSuggestedChangeSupport mySuggestedChangeSupport;

    @Inject
    public SandReviewService(Project project, ApplicationConcurrency applicationConcurrency) {
        myProject = project;
        myScheduler = applicationConcurrency.getScheduledExecutorService();
        myDisposable = Disposable.newDisposable("SandReviewService");
        Disposer.register(project, myDisposable);

        mySuggestedChangeSupport = new SandReviewSuggestedChangeSupport(project, this);
        myReviews = createReviews(project, myDisposable, myScheduler, mySuggestedChangeSupport);
        myToolwindowVm = new SandReviewToolwindowViewModel(new SandReviewProjectViewModel(myDisposable, myScheduler, () -> myReviews));
    }

    public SandReviewToolwindowViewModel getToolwindowVm() {
        return myToolwindowVm;
    }

    public List<SandReviewViewModel> getReviews() {
        return myReviews;
    }

    /**
     * Changes whenever a thread is started, so editors re-read {@link #getThreads}.
     */
    public ObservableValue<Integer> getThreadsModificationCount() {
        return myThreadsModificationCount;
    }

    /**
     * Starts a thread on a line of the first open review, as a review service would after a new line comment.
     */
    public void createThread(String fileName, int startLine, int line, SandReviewComment comment) {
        for (SandReviewViewModel review : myReviews) {
            if (review.getReviewRequestState().get() == ReviewRequestState.OPENED) {
                String trackingId = review.getNumber() + "-" + (review.getThreads().size() + 1);
                review.addThread(new SandReviewThreadViewModel(myProject, myDisposable, trackingId, fileName, startLine, line,
                    myScheduler, mySuggestedChangeSupport, List.of(comment)));
                myThreadsModificationCount.update(count -> count + 1);
                return;
            }
        }
    }

    public synchronized SandReviewDiffProcessorViewModel getDiffVm(String reviewNumber) {
        return myDiffVms.computeIfAbsent(reviewNumber, number -> {
            SandReviewDiffProcessorViewModel diffVm = new SandReviewDiffProcessorViewModel(myProject, this, myDisposable);
            diffVm.showChanges(ListSelection.createAt(SandReviewChangesViewModel.summaryChanges(myProject), 0));
            return diffVm;
        });
    }

    public SandReviewDiffVirtualFile getDiffFile(String reviewNumber) {
        return getDiffFile(reviewNumber, mySessionId);
    }

    public SandReviewDiffVirtualFile getDiffFile(String reviewNumber, String sessionId) {
        return new SandReviewDiffVirtualFile(sessionId, myProject, reviewNumber);
    }

    public Project getProject() {
        return myProject;
    }

    public SandReviewSuggestedChangeSupport getSuggestedChangeSupport() {
        return mySuggestedChangeSupport;
    }

    public Disposable getDisposable() {
        return myDisposable;
    }

    public ScheduledExecutorService getScheduler() {
        return myScheduler;
    }

    /**
     * @return the threads of open reviews anchored to files named {@code fileName}
     */
    public List<SandReviewThreadViewModel> getThreads(String fileName) {
        List<SandReviewThreadViewModel> result = new ArrayList<>();
        for (SandReviewViewModel review : myReviews) {
            if (review.getReviewRequestState().get() != ReviewRequestState.OPENED) {
                continue;
            }
            for (SandReviewThreadViewModel thread : review.getThreads()) {
                if (thread.getFileName().equals(fileName)) {
                    result.add(thread);
                }
            }
        }
        return result;
    }

    private static List<SandReviewViewModel> createReviews(Project project,
                                                           Disposable disposable,
                                                           ScheduledExecutorService scheduler,
                                                           SandReviewSuggestedChangeSupport support) {
        Instant now = Instant.now();
        SandReviewViewModel first = new SandReviewViewModel(
            project,
            disposable,
            scheduler,
            "42",
            "Add review API to the platform",
            "Adds the code review view models to the platform.",
            ReviewRequestState.OPENED,
            CURRENT_USER,
            Date.from(now.minus(Duration.ofDays(2))),
            false,
            List.of(
                new CodeReviewCIJob("build", CodeReviewCIJobState.SUCCESS, true, "https://ci.consulo.io/build/42"),
                new CodeReviewCIJob("tests", CodeReviewCIJobState.PENDING, true, "https://ci.consulo.io/tests/42")
            ),
            Map.of(REVIEWER, ReviewState.WAIT_FOR_UPDATES),
            List.of(
                new SandReviewThreadViewModel(project, disposable, "42-1", DEMO_FILE_NAME, 2, 4, scheduler, support, List.of(
                    new SandReviewComment(REVIEWER, "Should `map` keep a listener on the source forever?",
                        now.minus(Duration.ofHours(5))),
                    new SandReviewComment(CURRENT_USER, "Yes, same as the mutable value did before.", now.minus(Duration.ofHours(4)))
                )),
                new SandReviewThreadViewModel(project, disposable, "42-2", DEMO_FILE_NAME, 8, 8, scheduler, support, List.of(
                    new SandReviewComment(REVIEWER, "**Nit:** keep the javadoc of the api.\n\n![flow](" + SandReviewImages.DIAGRAM + ")",
                        now.minus(Duration.ofHours(3)))
                )),
                new SandReviewThreadViewModel(project, disposable, "42-3", DEMO_FILE_NAME, 13, 13, scheduler, support, List.of(
                    new SandReviewComment(REVIEWER, """
                        Say what the user gets:
                        ```suggestion
                        The sand review service rejects comments containing 'fail' with an error.
                        ```
                        The reply input shows it.""", now.minus(Duration.ofHours(2)))
                ))
            )
        );
        SandReviewViewModel second = new SandReviewViewModel(
            project,
            disposable,
            scheduler,
            "41",
            "Unified commit modes",
            "Commit in the Version Control tool window, a modal dialog or the Commit tool window.",
            ReviewRequestState.MERGED,
            REVIEWER,
            Date.from(now.minus(Duration.ofDays(5))),
            null,
            List.of(new CodeReviewCIJob("build", CodeReviewCIJobState.FAILED, true, null)),
            Map.of(CURRENT_USER, ReviewState.ACCEPTED),
            List.of(
                new SandReviewThreadViewModel(project, disposable, "41-1", "UnifiedCommitPanel.java", 119, 119, scheduler, support, List.of(
                    new SandReviewComment(CURRENT_USER, "Looks good on every frontend.", now.minus(Duration.ofDays(4)))
                ))
            )
        );
        SandReviewViewModel third = new SandReviewViewModel(
            project,
            disposable,
            scheduler,
            "40",
            "Draft: component inlays on Qt",
            "",
            ReviewRequestState.DRAFT,
            CURRENT_USER,
            Date.from(now.minus(Duration.ofDays(7))),
            true,
            List.of(),
            Map.of(),
            List.of()
        );
        third.setSeen(false);
        return List.of(first, second, third);
    }
}
