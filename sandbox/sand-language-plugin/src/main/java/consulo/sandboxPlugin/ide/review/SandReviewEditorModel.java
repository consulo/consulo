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
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.review.diff.DiscussionsViewOption;
import consulo.review.editor.CodeReviewChangedRange;
import consulo.review.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.review.editor.CodeReviewEditorGutterControlsState;
import consulo.review.editor.CodeReviewEditorModel;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewMultilineCommentableEditorModel;
import consulo.review.editor.CodeReviewNavigableEditorViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.editor.CodeReviewEditorDocumentTracker;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The review controls of an editor showing a sand demo file: the threads anchored to its lines and the inputs of new
 * line comments, which start a thread once sent.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewEditorModel implements CodeReviewEditorModel<CodeReviewInlayModel>, CodeReviewMultilineCommentableEditorModel,
    CodeReviewEditorGutterActionableChangesModel, CodeReviewNavigableEditorViewModel {
    private final SandReviewService myService;
    private final Editor myEditor;
    private final String myFileName;
    private final Disposable myDisposable;
    private final SandReviewInEditorViewModel myInEditorVm;

    private final Map<Object, SandReviewThreadInlayModel> myThreadInlays = new LinkedHashMap<>();
    private final List<SandReviewNewCommentInlayModel> myNewComments = new ArrayList<>();

    private final List<String> myBaseLines;
    private final MutableObservableValue<@Nullable List<CodeReviewChangedRange>> myReviewRanges = MutableObservableValue.of(null);
    private final MutableObservableValue<Boolean> myShouldHighlightDiffRanges = MutableObservableValue.of(false);

    private final MutableObservableValue<Collection<CodeReviewInlayModel>> myInlays = MutableObservableValue.of(List.of());
    private final MutableObservableValue<@Nullable CodeReviewEditorGutterControlsState> myGutterControlsState =
        MutableObservableValue.of(null);

    public SandReviewEditorModel(SandReviewService service,
                                 Editor editor,
                                 String fileName,
                                 SandReviewInEditorViewModel inEditorVm,
                                 Disposable parentDisposable) {
        myService = service;
        myEditor = editor;
        myFileName = fileName;
        myInEditorVm = inEditorVm;
        myDisposable = parentDisposable;

        Runnable viewOptionUnsubscribe = inEditorVm.getDiscussionsViewOption().addListener(option -> publish());
        Disposer.register(parentDisposable, viewOptionUnsubscribe::run);

        Runnable unsubscribe = service.getThreadsModificationCount().addListener(count -> reloadThreads());
        Disposer.register(parentDisposable, unsubscribe::run);
        reloadThreads();

        myBaseLines = baseLines(editor.getDocument().getText());
        Application.get().getInstance(CodeReviewEditorDocumentTracker.class)
            .trackDocumentDiff(parentDisposable, String.join("\n", myBaseLines), editor.getDocument(), ranges -> myReviewRanges.set(List.copyOf(ranges)));
    }

    private static List<String> baseLines(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        if (lines.size() < 13) {
            return lines;
        }
        List<String> base = new ArrayList<>(lines.subList(0, 2));
        base.add("ObservableValue keeps listeners per value.");
        base.add("Each listener sees every value.");
        base.addAll(lines.subList(5, 8));
        base.add("line 9 has a typo in the javadoc.");
        base.addAll(lines.subList(9, 11));
        base.addAll(lines.subList(12, lines.size()));
        return base;
    }

    private synchronized void reloadThreads() {
        for (SandReviewThreadViewModel thread : myService.getThreads(myFileName)) {
            myThreadInlays.computeIfAbsent(thread.getTrackingId(), key -> {
                SandReviewThreadInlayModel inlay = new SandReviewThreadInlayModel(thread);
                Runnable unsubscribe = inlay.isVisible().addListener(visible -> publish());
                Disposer.register(myDisposable, unsubscribe::run);
                return inlay;
            });
        }
        publish();
    }

    private synchronized void publish() {
        DiscussionsViewOption viewOption = myInEditorVm.getDiscussionsViewOption().get();
        List<CodeReviewInlayModel> inlays = new ArrayList<>();
        for (SandReviewThreadInlayModel inlay : myThreadInlays.values()) {
            if (viewOption == DiscussionsViewOption.ALL
                || viewOption == DiscussionsViewOption.UNRESOLVED_ONLY && !((SandReviewThreadViewModel) inlay.getThread()).isResolved().get()) {
                inlays.add(inlay);
            }
        }
        inlays.addAll(myNewComments);
        myInlays.set(List.copyOf(inlays));

        Set<Integer> linesWithComments = new LinkedHashSet<>();
        for (SandReviewThreadInlayModel inlay : myThreadInlays.values()) {
            linesWithComments.add(inlay.getLine().get());
        }
        Set<Integer> linesWithNewComments = new LinkedHashSet<>();
        for (SandReviewNewCommentInlayModel inlay : myNewComments) {
            linesWithNewComments.add(inlay.getLine().get());
        }
        myGutterControlsState.set(new SandReviewGutterControlsState(Set.copyOf(linesWithComments), Set.copyOf(linesWithNewComments)));
    }

    @Override
    public ObservableValue<@Nullable List<CodeReviewChangedRange>> getReviewRanges() {
        return myReviewRanges;
    }

    @RequiredUIAccess
    @Override
    public boolean getShouldHighlightDiffRanges() {
        return myShouldHighlightDiffRanges.get();
    }

    @RequiredUIAccess
    @Override
    public void setShouldHighlightDiffRanges(boolean value) {
        myShouldHighlightDiffRanges.set(value);
    }

    @RequiredUIAccess
    @Override
    public @Nullable String getBaseContent(int baseLine1, int baseLine2) {
        if (baseLine1 < 0 || baseLine2 > myBaseLines.size() || baseLine1 >= baseLine2) {
            return null;
        }
        return String.join("\n", myBaseLines.subList(baseLine1, baseLine2)) + "\n";
    }

    @RequiredUIAccess
    @Override
    public void showDiff(@Nullable Integer lineIdx) {
    }

    @RequiredUIAccess
    @Override
    public void addDiffHighlightListener(Disposable disposable, Runnable listener) {
        Runnable unsubscribe = myShouldHighlightDiffRanges.addListener(value -> listener.run());
        Disposer.register(disposable, unsubscribe::run);
    }

    @Override
    public ObservableValue<Collection<CodeReviewInlayModel>> getInlays() {
        return myInlays;
    }

    @Override
    public ObservableValue<@Nullable CodeReviewEditorGutterControlsState> getGutterControlsState() {
        return myGutterControlsState;
    }

    @RequiredUIAccess
    @Override
    public synchronized void toggleComments(int lineIdx) {
        for (SandReviewThreadInlayModel inlay : myThreadInlays.values()) {
            if (inlay.getLine().get() == lineIdx) {
                inlay.toggleVisible();
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void requestNewComment(int lineIdx) {
        requestNewComment(new CodeReviewLineRange(lineIdx, lineIdx));
    }

    @RequiredUIAccess
    @Override
    public boolean canCreateComment(CodeReviewLineRange lineRange) {
        return true;
    }

    @RequiredUIAccess
    @Override
    public synchronized void requestNewComment(CodeReviewLineRange lineRange) {
        if (findNewComment(lineRange.end()) != null) {
            return;
        }
        MutableObservableValue<CodeReviewLineRange> range = MutableObservableValue.of(lineRange);
        AtomicReference<@Nullable SandReviewNewCommentInlayModel> inlay = new AtomicReference<>();
        SandReviewNewCommentViewModel editor = new SandReviewNewCommentViewModel(
            myService.getProject(),
            myDisposable,
            myService.getScheduler(),
            comment -> {
                removeNewComment(inlay.get());
                CodeReviewLineRange commented = range.get();
                myService.createThread(myFileName, commented.start(), commented.end(), comment);
            },
            () -> createSuggestedChange(range.get())
        );
        inlay.set(new SandReviewNewCommentInlayModel(range,
            editor,
            myService.getSuggestedChangeSupport().getSyntax(),
            () -> removeNewComment(inlay.get()),
            this::publish));
        myNewComments.add(inlay.get());
        publish();
    }

    private @Nullable SandReviewNewCommentInlayModel findNewComment(int lineIdx) {
        for (SandReviewNewCommentInlayModel inlay : myNewComments) {
            if (inlay.getLine().get() == lineIdx) {
                return inlay;
            }
        }
        return null;
    }

    private synchronized void removeNewComment(@Nullable SandReviewNewCommentInlayModel inlay) {
        if (inlay != null && myNewComments.remove(inlay)) {
            publish();
        }
    }

    private String createSuggestedChange(CodeReviewLineRange lineRange) {
        Document document = myEditor.getDocument();
        List<String> lines = new ArrayList<>();
        for (int i = lineRange.start(); i <= lineRange.end() && i < document.getLineCount(); i++) {
            lines.add(document.getText(new TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i))));
        }
        return myService.getSuggestedChangeSupport().getSyntax().createSuggestion(lines);
    }

    @RequiredUIAccess
    @Override
    public synchronized void cancelNewComment(int lineIdx) {
        removeNewComment(findNewComment(lineIdx));
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public boolean canGotoNextComment(String threadId) {
        Integer line = lineOf(threadId);
        return line != null && canGotoNextComment(line);
    }

    @RequiredUIAccess
    @Override
    public boolean canGotoNextComment(int line) {
        return findLine(line, true) != null;
    }

    @RequiredUIAccess
    @Override
    public boolean canGotoPreviousComment(String threadId) {
        Integer line = lineOf(threadId);
        return line != null && canGotoPreviousComment(line);
    }

    @RequiredUIAccess
    @Override
    public boolean canGotoPreviousComment(int line) {
        return findLine(line, false) != null;
    }

    @RequiredUIAccess
    @Override
    public void gotoNextComment(String threadId) {
        Integer line = lineOf(threadId);
        if (line != null) {
            gotoNextComment(line);
        }
    }

    @RequiredUIAccess
    @Override
    public void gotoNextComment(int line) {
        gotoLine(findLine(line, true));
    }

    @RequiredUIAccess
    @Override
    public void gotoPreviousComment(String threadId) {
        Integer line = lineOf(threadId);
        if (line != null) {
            gotoPreviousComment(line);
        }
    }

    @RequiredUIAccess
    @Override
    public void gotoPreviousComment(int line) {
        gotoLine(findLine(line, false));
    }

    private synchronized @Nullable Integer lineOf(String threadId) {
        SandReviewThreadInlayModel inlay = myThreadInlays.get(threadId);
        return inlay == null ? null : inlay.getLine().get();
    }

    private synchronized @Nullable Integer findLine(int line, boolean next) {
        Integer result = null;
        for (SandReviewThreadInlayModel inlay : myThreadInlays.values()) {
            int threadLine = inlay.getLine().get();
            boolean candidate = next ? threadLine > line : threadLine < line;
            boolean closer = result == null || (next ? threadLine < result : threadLine > result);
            if (candidate && closer) {
                result = threadLine;
            }
        }
        return result;
    }

    @RequiredUIAccess
    private void gotoLine(@Nullable Integer line) {
        if (line == null) {
            return;
        }
        myEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
        myEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }
}
