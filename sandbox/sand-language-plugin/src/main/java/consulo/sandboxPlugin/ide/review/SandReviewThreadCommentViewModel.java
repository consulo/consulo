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

import consulo.review.comment.CodeReviewCommentBodyBlock;
import consulo.review.comment.CodeReviewTextEditingViewModel;
import consulo.review.list.UserPresentation;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModel;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.List;

/**
 * A comment of a thread: its body is split into markdown and suggested changes of the line the thread is on.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public class SandReviewThreadCommentViewModel implements CodeReviewCommentViewModel {
    private final SandReviewComment myComment;
    private final SandReviewThreadViewModel myThread;
    private @Nullable CodeReviewCommentBodyViewModel myBodyVm;
    private final MutableObservableValue<String> myBody;
    private final MutableObservableValue<@Nullable CodeReviewTextEditingViewModel> myEditVm = MutableObservableValue.of(null);

    public SandReviewThreadCommentViewModel(SandReviewComment comment, SandReviewThreadViewModel thread) {
        myComment = comment;
        myThread = thread;
        myBody = MutableObservableValue.of(comment.text());
    }

    @Override
    public String getId() {
        return myComment.getId();
    }

    @Override
    public UserPresentation getAuthor() {
        return myComment.getAuthor();
    }

    @Override
    public Date getCreatedAt() {
        return myComment.getCreatedAt();
    }

    @Override
    public ObservableValue<String> getBody() {
        return myBody;
    }

    @Override
    public ObservableValue<List<CodeReviewCommentBodyBlock>> getBodyBlocks() {
        CodeReviewCommentBodyViewModel bodyVm = myBodyVm;
        if (bodyVm == null) {
            bodyVm = myThread.createBodyViewModel(getBody());
            myBodyVm = bodyVm;
        }
        return bodyVm.getBlocks();
    }

    @Override
    public boolean isOwn() {
        return myComment.isOwn();
    }

    @Override
    public boolean canEdit() {
        return isOwn();
    }

    @Override
    public ObservableValue<@Nullable CodeReviewTextEditingViewModel> getEditVm() {
        return myEditVm;
    }

    @Override
    public void editBody() {
        if (myEditVm.get() != null) {
            return;
        }
        myEditVm.set(new SandReviewEditCommentViewModel(
            myThread.getProject(),
            myThread.getParentDisposable(),
            myThread.getScheduler(),
            myBody.get(),
            text -> {
                myBody.set(text);
                myThread.updateComment(getId(), text);
            },
            () -> myEditVm.set(null)
        ));
    }

    @Override
    public boolean canDelete() {
        return isOwn();
    }

    @Override
    public void delete() {
        myThread.removeComment(getId());
    }
}
