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
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.review.timeline.thread.DefaultRepliesStateData;
import consulo.review.timeline.thread.RepliesStateData;
import consulo.review.user.CodeReviewUser;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A sand review thread on a line of a file - the file is matched by name, so any file of that name shows it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewThreadViewModel implements CodeReviewThreadViewModel {
    private final String myTrackingId;
    private final String myFileName;
    private final int myStartLine;
    private final int myLine;
    private final ScheduledExecutorService myScheduler;
    private final MutableObservableValue<List<SandReviewComment>> myComments;
    private final ObservableValue<List<CodeReviewCommentViewModel>> myCommentsView;
    private final MutableObservableValue<Boolean> myResolved = MutableObservableValue.of(false);
    private final MutableObservableValue<Boolean> myBusy = MutableObservableValue.of(false);
    private final MutableObservableValue<Boolean> myRepliesFolded = MutableObservableValue.of(true);
    private final ObservableValue<RepliesStateData> myRepliesState;
    private final SandReviewNewCommentViewModel myReplyVm;
    private final Project myProject;
    private final MutableObservableValue<Boolean> myShownInEditor = MutableObservableValue.of(true);

    public SandReviewThreadViewModel(Project project,
                                     Disposable parentDisposable,
                                     String trackingId,
                                     String fileName,
                                     int startLine,
                                     int line,
                                     ScheduledExecutorService scheduler,
                                     List<SandReviewComment> comments) {
        myProject = project;
        myTrackingId = trackingId;
        myFileName = fileName;
        myStartLine = startLine;
        myLine = line;
        myScheduler = scheduler;
        myComments = MutableObservableValue.of(List.copyOf(comments));
        myCommentsView = myComments.map(List::copyOf);
        myRepliesState = myComments.map(SandReviewThreadViewModel::toRepliesState);
        myReplyVm = new SandReviewNewCommentViewModel(project, parentDisposable, scheduler, this::addComment);
    }

    public String getFileName() {
        return myFileName;
    }

    /**
     * @return the 0-based last line the thread is about, which it is anchored under
     */
    public int getLine() {
        return myLine;
    }

    /**
     * @return the 0-based first line the thread is about
     */
    public int getStartLine() {
        return myStartLine;
    }

    @Override
    public String getLocation() {
        return myStartLine == myLine ? myFileName + ":" + (myLine + 1) : myFileName + ":" + (myStartLine + 1) + "-" + (myLine + 1);
    }

    /**
     * Whether editors show the thread under its line; the gutter toggle flips it.
     */
    public MutableObservableValue<Boolean> getShownInEditor() {
        return myShownInEditor;
    }

    @Override
    public boolean canNavigate() {
        return findFile() != null;
    }

    @RequiredUIAccess
    @Override
    public void navigate() {
        VirtualFile file = findFile();
        if (file == null) {
            return;
        }
        myShownInEditor.set(true);
        OpenFileDescriptorFactory.getInstance(myProject).newBuilder(file).line(myLine).build().navigate(true);
    }

    private @Nullable VirtualFile findFile() {
        VirtualFile baseDir = myProject.getBaseDir();
        return baseDir == null ? null : baseDir.findChild(myFileName);
    }

    @Override
    public ObservableValue<List<CodeReviewCommentViewModel>> getComments() {
        return myCommentsView;
    }

    public void addComment(SandReviewComment comment) {
        myComments.update(comments -> {
            List<SandReviewComment> result = new ArrayList<>(comments);
            result.add(comment);
            return List.copyOf(result);
        });
    }

    @Override
    public CodeReviewNewCommentViewModel getReplyVm() {
        return myReplyVm;
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
