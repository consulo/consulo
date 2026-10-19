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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.diff.util.Side;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CollapsibleTimelineItemViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.review.timeline.thread.DefaultRepliesStateData;
import consulo.review.timeline.thread.RepliesStateData;
import consulo.review.user.CodeReviewUser;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchLine;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModel;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModelFactory;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionThreadData;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import consulo.versionControlSystem.review.timeline.TimelineDiffComponentFactory;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A sand review thread on a line of a file - the file is matched by name, so any file of that name shows it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewThreadViewModel implements CodeReviewThreadViewModel, CollapsibleTimelineItemViewModel {
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
    private final Disposable myParentDisposable;
    private final SandReviewSuggestedChangeSupport mySuggestedChangeSupport;
    private final MutableObservableValue<Boolean> myShownInEditor = MutableObservableValue.of(true);
    private final Map<String, SandReviewThreadCommentViewModel> myCommentViewModels = new HashMap<>();
    private final MutableObservableValue<@Nullable CodeReviewSuggestionThreadData> mySuggestionThreadData = MutableObservableValue.of(null);
    private volatile @Nullable List<String> myFileLines;
    private final MutableObservableValue<Boolean> myCollapsed;

    public SandReviewThreadViewModel(Project project,
                                     Disposable parentDisposable,
                                     String trackingId,
                                     String fileName,
                                     int startLine,
                                     int line,
                                     ScheduledExecutorService scheduler,
                                     SandReviewSuggestedChangeSupport suggestedChangeSupport,
                                     List<SandReviewComment> comments) {
        myProject = project;
        myParentDisposable = parentDisposable;
        mySuggestedChangeSupport = suggestedChangeSupport;
        myTrackingId = trackingId;
        myFileName = fileName;
        myStartLine = startLine;
        myLine = line;
        myScheduler = scheduler;
        myComments = MutableObservableValue.of(List.copyOf(comments));
        myCommentsView = myComments.map(this::toViewModels);
        myRepliesState = myComments.map(SandReviewThreadViewModel::toRepliesState);
        myReplyVm = new SandReviewNewCommentViewModel(project, parentDisposable, scheduler, this::addComment, this::createSuggestedChange);
        myCollapsed = MutableObservableValue.of(myResolved.get());
        myResolved.addListener(resolved -> updateSuggestionThreadData());
        loadFileLines();
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

    private synchronized List<CodeReviewCommentViewModel> toViewModels(List<SandReviewComment> comments) {
        List<CodeReviewCommentViewModel> result = new ArrayList<>(comments.size());
        for (SandReviewComment comment : comments) {
            result.add(myCommentViewModels.computeIfAbsent(comment.getId(), id -> new SandReviewThreadCommentViewModel(comment, this)));
        }
        return List.copyOf(result);
    }

    public CodeReviewCommentBodyViewModel createBodyViewModel(ObservableValue<String> body) {
        return myProject.getInstance(CodeReviewCommentBodyViewModelFactory.class)
            .create(myParentDisposable, mySuggestedChangeSupport, mySuggestionThreadData, body);
    }

    /**
     * Reads the lines of the file off the UI thread, the way a review service sends the diff hunk a thread is on.
     */
    private void loadFileLines() {
        myScheduler.execute(() -> {
            Supplier<@Nullable String> readText = () -> {
                VirtualFile file = findFile();
                Document document = file == null ? null : FileDocumentManager.getInstance().getDocument(file);
                return document == null ? null : document.getText();
            };
            String text = Application.get().runReadAction(readText);
            if (text != null) {
                myFileLines = List.of(text.split("\n", -1));
                updateSuggestionThreadData();
            }
        });
    }

    private void updateSuggestionThreadData() {
        PatchHunk hunk = createHunk();
        if (hunk == null) {
            return;
        }
        StringBuilder diffHunk = new StringBuilder();
        int count = hunk.getEndLineBefore() - hunk.getStartLineBefore();
        diffHunk.append("@@ -").append(hunk.getStartLineBefore() + 1).append(',').append(count)
            .append(" +").append(hunk.getStartLineAfter() + 1).append(',').append(count).append(" @@\n");
        for (PatchLine line : hunk.getLines()) {
            diffHunk.append(' ').append(line.getText()).append('\n');
        }
        mySuggestionThreadData.set(new CodeReviewSuggestionThreadData(myTrackingId,
            myResolved.get(),
            true,
            myFileName,
            diffHunk.toString(),
            myLine - myStartLine + 1,
            myStartLine,
            myLine,
            myStartLine,
            myLine));
    }

    /**
     * The lines of the thread and {@link TimelineDiffComponentFactory#DIFF_CONTEXT_SIZE} lines above them, unchanged.
     */
    private @Nullable PatchHunk createHunk() {
        List<String> lines = myFileLines;
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        int from = Math.max(0, myStartLine - TimelineDiffComponentFactory.DIFF_CONTEXT_SIZE);
        int to = Math.min(myLine, lines.size() - 1);
        PatchHunk hunk = new PatchHunk(from, to + 1, from, to + 1);
        for (int i = from; i <= to; i++) {
            hunk.addLine(new PatchLine(PatchLine.Type.CONTEXT, lines.get(i)));
        }
        return hunk;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createDiffComponent(Disposable parentDisposable) {
        PatchHunk hunk = createHunk();
        if (hunk == null) {
            return null;
        }

        TimelineDiffComponentFactory factory = myProject.getInstance(TimelineDiffComponentFactory.class);
        return factory.createDiffWithHeader(parentDisposable,
            this,
            myFileName,
            ObservableValue.of(this::navigate),
            disposable -> factory.createDiffComponent(disposable,
                hunk,
                new DiffLineLocation(Side.RIGHT, myLine),
                new DiffLineLocation(Side.RIGHT, myStartLine)));
    }

    @Override
    public ObservableValue<Boolean> getCollapsible() {
        return ObservableValue.of(true);
    }

    @Override
    public ObservableValue<Boolean> getCollapsed() {
        return myCollapsed;
    }

    @Override
    public void setCollapsed(boolean collapsed) {
        myCollapsed.set(collapsed);
    }

    private String createSuggestedChange() {
        List<String> fileLines = myFileLines;
        List<String> lines = new ArrayList<>();
        if (fileLines != null) {
            for (int i = myStartLine; i <= myLine && i < fileLines.size(); i++) {
                lines.add(fileLines.get(i));
            }
        }
        return mySuggestedChangeSupport.getSyntax().createSuggestion(lines);
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
