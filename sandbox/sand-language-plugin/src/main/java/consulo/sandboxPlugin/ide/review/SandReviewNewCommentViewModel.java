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
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.comment.CodeReviewSubmittableTextViewModelBase;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A new comment of the sand review service - text containing "fail" is rejected, to show the error state.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewNewCommentViewModel extends CodeReviewSubmittableTextViewModelBase implements CodeReviewNewCommentViewModel {
    private final Consumer<SandReviewComment> mySink;
    private final ScheduledExecutorService myScheduler;
    private final @Nullable Supplier<String> mySuggestedChange;

    public SandReviewNewCommentViewModel(Project project,
                                         Disposable parentDisposable,
                                         ScheduledExecutorService scheduler,
                                         Consumer<SandReviewComment> sink) {
        this(project, parentDisposable, scheduler, sink, null);
    }

    /**
     * @param suggestedChange a suggestion of the commented lines, {@code null} when the comment is on no lines
     */
    public SandReviewNewCommentViewModel(Project project,
                                         Disposable parentDisposable,
                                         ScheduledExecutorService scheduler,
                                         Consumer<SandReviewComment> sink,
                                         @Nullable Supplier<String> suggestedChange) {
        super(project, parentDisposable, "");
        mySink = sink;
        myScheduler = scheduler;
        mySuggestedChange = suggestedChange;
    }

    @Override
    public boolean canSuggestChange() {
        return mySuggestedChange != null;
    }

    @Override
    public String createSuggestedChange() {
        Supplier<String> suggestedChange = mySuggestedChange;
        return suggestedChange == null ? "" : suggestedChange.get();
    }

    @Override
    public void submit() {
        submit(text -> {
            CompletableFuture<Object> result = new CompletableFuture<>();
            myScheduler.schedule(() -> {
                if (text.isBlank() || text.contains("fail")) {
                    result.completeExceptionally(new IllegalArgumentException("The sand review service rejected the comment"));
                    return;
                }
                mySink.accept(new SandReviewComment(SandReviewService.CURRENT_USER, text, Instant.now()));
                getText().set("");
                result.complete(text);
            }, 800, TimeUnit.MILLISECONDS);
            return result;
        });
    }
}
