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

import consulo.project.Project;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.PatchApplyService;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeSupport;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionSyntax;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Sand comments write suggestions the way GitHub does. There is no repository to commit to, so a commit applies the
 * suggestion and answers success.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public class SandReviewSuggestedChangeSupport implements CodeReviewSuggestedChangeSupport {
    private final Project myProject;
    private final SandReviewService myService;

    public SandReviewSuggestedChangeSupport(Project project, SandReviewService service) {
        myProject = project;
        myService = service;
    }

    @Override
    public CodeReviewSuggestionSyntax getSyntax() {
        return CodeReviewSuggestionSyntax.GITHUB;
    }

    @Override
    public VirtualFile getRepositoryRoot() {
        return Objects.requireNonNull(myProject.getBaseDir());
    }

    @Override
    public boolean canCommit() {
        return true;
    }

    @Override
    public CompletableFuture<ApplyPatchStatus> commit(TextFilePatch patch, String commitMessage) {
        Supplier<CompletableFuture<ApplyPatchStatus>> apply =
            () -> myProject.getInstance(PatchApplyService.class).apply(getRepositoryRoot(), List.of(patch), true);
        return myProject.getUIAccess().giveAsync(apply).thenCompose(status -> status);
    }

    @Override
    public CompletableFuture<?> resolveThread(String threadId) {
        for (SandReviewViewModel review : myService.getReviews()) {
            for (SandReviewThreadViewModel thread : review.getThreads()) {
                if (thread.getTrackingId().equals(threadId) && !thread.isResolved().get()) {
                    thread.changeResolvedState();
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
