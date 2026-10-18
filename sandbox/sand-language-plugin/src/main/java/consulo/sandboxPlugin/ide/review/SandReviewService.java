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

import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.details.data.ReviewRequestState;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class SandReviewService {
    public static final SandReviewUser CURRENT_USER = new SandReviewUser("vistall", "Valery Semenchuk");
    public static final SandReviewUser REVIEWER = new SandReviewUser("sand-bot", "Sand Reviewer");

    private SandReviewService() {
    }

    public static SandReviewToolwindowViewModel create(ScheduledExecutorService scheduler) {
        List<SandReviewViewModel> reviews = createReviews(scheduler);
        SandReviewListViewModel listVm = new SandReviewListViewModel(scheduler, () -> reviews);
        return new SandReviewToolwindowViewModel(new SandReviewProjectViewModel(listVm));
    }

    private static List<SandReviewViewModel> createReviews(ScheduledExecutorService scheduler) {
        Instant now = Instant.now();
        SandReviewViewModel first = new SandReviewViewModel(
            "42",
            "Add review API to the platform",
            "Ports the code review view models from JetBrains collaboration tools.",
            ReviewRequestState.OPENED,
            CURRENT_USER,
            Date.from(now.minus(Duration.ofDays(2))),
            false,
            List.of(
                new CodeReviewCIJob("build", CodeReviewCIJobState.SUCCESS, true, null),
                new CodeReviewCIJob("tests", CodeReviewCIJobState.PENDING, true, null)
            ),
            List.of(
                new SandReviewThreadViewModel("42-1", "ObservableValue.java:34", scheduler, List.of(
                    new SandReviewComment(REVIEWER, "Should `map` keep a listener on the source forever?", now.minus(Duration.ofHours(5))),
                    new SandReviewComment(CURRENT_USER, "Yes, same as the mutable value did before.", now.minus(Duration.ofHours(4)))
                )),
                new SandReviewThreadViewModel("42-2", "ReviewListViewModel.java:12", scheduler, List.of(
                    new SandReviewComment(REVIEWER, "**Nit:** javadoc came from the JetBrains source, keep it.", now.minus(Duration.ofHours(3)))
                ))
            )
        );
        SandReviewViewModel second = new SandReviewViewModel(
            "41",
            "Unified commit modes",
            "Commit in the Version Control tool window, a modal dialog or the Commit tool window.",
            ReviewRequestState.MERGED,
            REVIEWER,
            Date.from(now.minus(Duration.ofDays(5))),
            null,
            List.of(new CodeReviewCIJob("build", CodeReviewCIJobState.FAILED, true, null)),
            List.of(
                new SandReviewThreadViewModel("41-1", "UnifiedCommitPanel.java:120", scheduler, List.of(
                    new SandReviewComment(CURRENT_USER, "Looks good on every frontend.", now.minus(Duration.ofDays(4)))
                ))
            )
        );
        SandReviewViewModel third = new SandReviewViewModel(
            "40",
            "Draft: component inlays on Qt",
            "",
            ReviewRequestState.DRAFT,
            CURRENT_USER,
            Date.from(now.minus(Duration.ofDays(7))),
            true,
            List.of(),
            List.of()
        );
        return List.of(first, second, third);
    }
}
