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
package consulo.ai.editor.impl.internal.plan;

import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.review.timeline.thread.RepliesStateData;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;

/**
 * A comment on lines of a plan, shown as a thread of one comment - no replies, nothing to resolve.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIPlanThread implements CodeReviewThreadViewModel {
    private final AIPlanComment myComment;

    AIPlanThread(AIPlanComment comment) {
        myComment = comment;
    }

    @Override
    public ObservableValue<List<CodeReviewCommentViewModel>> getComments() {
        return ObservableValue.of(List.of(myComment));
    }

    @Override
    public ObservableValue<Boolean> isResolved() {
        return ObservableValue.of(false);
    }

    @Override
    public ObservableValue<Boolean> getCanChangeResolvedState() {
        return ObservableValue.of(false);
    }

    @Override
    public ObservableValue<Boolean> isBusy() {
        return ObservableValue.of(false);
    }

    @Override
    public void changeResolvedState() {
    }

    @Override
    public ObservableValue<RepliesStateData> getRepliesState() {
        return ObservableValue.of(RepliesStateData.EMPTY);
    }

    @Override
    public ObservableValue<Boolean> getCanCreateReplies() {
        return ObservableValue.of(false);
    }

    @Override
    public ObservableValue<Boolean> getRepliesFolded() {
        return ObservableValue.of(false);
    }

    @Override
    public void unfoldReplies() {
    }

    @Override
    public String getTrackingId() {
        return myComment.getId();
    }
}
