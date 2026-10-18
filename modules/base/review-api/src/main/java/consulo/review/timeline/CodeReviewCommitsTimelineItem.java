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
package consulo.review.timeline;

import consulo.review.list.UserPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.List;

/**
 * Commits added to a review, shown in the timeline as one entry: who added them, when, and every commit.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewCommitsTimelineItem extends CodeReviewTimelineItem {
    UserPresentation getAuthor();

    @Nullable Date getCreatedAt();

    List<CodeReviewTimelineCommit> getCommits();

    /**
     * Opens the commit in the review - the hash of a commit is a link to it.
     */
    @RequiredUIAccess
    void showCommit(String hash);
}
