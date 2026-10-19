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
package consulo.versionControlSystem.review.comment;

import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.virtualFileSystem.VirtualFile;

import java.util.concurrent.CompletableFuture;

/**
 * What a review platform tells about the suggested changes of its comments: how they are written and what can be
 * done with them. A platform passing none shows its comments as markdown alone.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public interface CodeReviewSuggestedChangeSupport {
    CodeReviewSuggestionSyntax getSyntax();

    /**
     * @return the directory the file paths of the review are relative to
     */
    VirtualFile getRepositoryRoot();

    /**
     * Whether a suggestion can be applied to the local files. Without it the block shows the change alone.
     */
    default boolean canApplyLocally() {
        return true;
    }

    /**
     * Whether the branch of the review is the current one - a suggestion is applied to that one only.
     */
    default ObservableValue<Boolean> isOnReviewBranch() {
        return ObservableValue.of(true);
    }

    /**
     * Whether a suggestion can be committed at once. Without it only applying is offered.
     */
    default boolean canCommit() {
        return false;
    }

    /**
     * Applies the patch and commits the file it changes with the message.
     */
    default CompletableFuture<ApplyPatchStatus> commit(TextFilePatch patch, String commitMessage) {
        return CompletableFuture.completedFuture(ApplyPatchStatus.FAILURE);
    }

    /**
     * Resolves the thread once its suggestion was applied or committed.
     */
    default CompletableFuture<?> resolveThread(String threadId) {
        return CompletableFuture.completedFuture(null);
    }
}
