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

import consulo.review.timeline.thread.CodeReviewFoldableThreadViewModel;
import consulo.review.timeline.thread.CodeReviewResolvableItemViewModel;
import consulo.review.timeline.thread.CodeReviewTrackableItemViewModel;
import consulo.review.timeline.thread.DefaultRepliesStateData;
import consulo.review.timeline.thread.RepliesStateData;
import consulo.review.user.CodeReviewUser;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewThreadViewModel implements CodeReviewResolvableItemViewModel,
    CodeReviewFoldableThreadViewModel,
    CodeReviewTrackableItemViewModel {
    private final String myTrackingId;
    private final String myLocation;
    private final ScheduledExecutorService myScheduler;
    private final MutableObservableValue<List<SandReviewComment>> myComments;
    private final MutableObservableValue<Boolean> myResolved = MutableObservableValue.of(false);
    private final MutableObservableValue<Boolean> myBusy = MutableObservableValue.of(false);
    private final MutableObservableValue<Boolean> myRepliesFolded = MutableObservableValue.of(true);
    private final ObservableValue<RepliesStateData> myRepliesState;

    public SandReviewThreadViewModel(String trackingId, String location, ScheduledExecutorService scheduler, List<SandReviewComment> comments) {
        myTrackingId = trackingId;
        myLocation = location;
        myScheduler = scheduler;
        myComments = MutableObservableValue.of(List.copyOf(comments));
        myRepliesState = myComments.map(SandReviewThreadViewModel::toRepliesState);
    }

    public String getLocation() {
        return myLocation;
    }

    public ObservableValue<List<SandReviewComment>> getComments() {
        return myComments;
    }

    public void addComment(SandReviewComment comment) {
        myComments.update(comments -> {
            List<SandReviewComment> result = new ArrayList<>(comments);
            result.add(comment);
            return List.copyOf(result);
        });
    }

    @Override
    public String getTrackingId() {
        return myTrackingId;
    }

    @Override
    public ObservableValue<Boolean> isResolved() {
        return myResolved;
    }

    @Override
    public ObservableValue<Boolean> getCanChangeResolvedState() {
        return ObservableValue.of(true);
    }

    @Override
    public ObservableValue<Boolean> isBusy() {
        return myBusy;
    }

    @Override
    public void changeResolvedState() {
        if (!myBusy.compareAndSet(false, true)) {
            return;
        }
        myScheduler.schedule(() -> {
            myResolved.update(resolved -> !resolved);
            myBusy.set(false);
        }, 600, TimeUnit.MILLISECONDS);
    }

    @Override
    public ObservableValue<RepliesStateData> getRepliesState() {
        return myRepliesState;
    }

    @Override
    public ObservableValue<Boolean> getCanCreateReplies() {
        return ObservableValue.of(true);
    }

    @Override
    public ObservableValue<Boolean> getRepliesFolded() {
        return myRepliesFolded;
    }

    @Override
    public void unfoldReplies() {
        myRepliesFolded.set(false);
    }

    private static RepliesStateData toRepliesState(List<SandReviewComment> comments) {
        if (comments.size() < 2) {
            return RepliesStateData.EMPTY;
        }
        Set<CodeReviewUser> authors = new LinkedHashSet<>();
        for (SandReviewComment reply : comments.subList(1, comments.size())) {
            authors.add(reply.author());
        }
        Instant last = comments.get(comments.size() - 1).time();
        return new DefaultRepliesStateData(authors, comments.size() - 1, Date.from(last));
    }
}
