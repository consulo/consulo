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
import consulo.project.Project;
import consulo.review.comment.CodeReviewSubmittableTextViewModelBase;
import consulo.review.comment.CodeReviewTextEditingViewModel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Edits the body of a comment of the sand review service - saving is delayed like a request, and rejected when the new
 * text asks to fail.
 */
public class SandReviewEditCommentViewModel extends CodeReviewSubmittableTextViewModelBase implements CodeReviewTextEditingViewModel {
    private final ScheduledExecutorService myScheduler;
    private final Consumer<String> mySaver;
    private final Runnable myStop;

    public SandReviewEditCommentViewModel(Project project,
                                          Disposable parentDisposable,
                                          ScheduledExecutorService scheduler,
                                          String initialText,
                                          Consumer<String> saver,
                                          Runnable stop) {
        super(project, parentDisposable, initialText);
        myScheduler = scheduler;
        mySaver = saver;
        myStop = stop;
    }

    @Override
    public void save() {
        submit(text -> {
            CompletableFuture<Object> result = new CompletableFuture<>();
            myScheduler.schedule(() -> {
                if (text.isBlank() || text.contains("fail")) {
                    result.completeExceptionally(new IllegalArgumentException("The sand review service rejected the comment"));
                    return;
                }
                mySaver.accept(text);
                result.complete(text);
                stopEditing();
            }, 500, TimeUnit.MILLISECONDS);
            return result;
        });
    }

    @Override
    public void stopEditing() {
        myStop.run();
        Disposer.dispose(this);
    }
}
