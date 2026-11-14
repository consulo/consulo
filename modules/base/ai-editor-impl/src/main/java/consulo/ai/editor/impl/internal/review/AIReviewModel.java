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
package consulo.ai.editor.impl.internal.review;

import consulo.ai.editor.impl.internal.review.state.AIPlanReviewDraftEntry;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.project.Project;
import consulo.review.editor.CodeReviewEditorGutterControlsState;
import consulo.review.editor.CodeReviewEditorModel;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewMultilineCommentableEditorModel;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The review of a plan the agent wrote - comments on its lines, kept as drafts, which go to the agent together as the
 * feedback on the plan. The editor shows them as the comments of a code review.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIReviewModel implements CodeReviewEditorModel<CodeReviewInlayModel>, CodeReviewMultilineCommentableEditorModel, Disposable {
    private static final int MAX_QUOTE = 80;

    private final Project myProject;
    private final Document myDocument;
    private final Runnable myChanged;
    private final List<AIReviewDraftInlay> myDrafts = new ArrayList<>();
    private final MutableObservableValue<Collection<CodeReviewInlayModel>> myInlays = MutableObservableValue.of(List.of());
    private final MutableObservableValue<@Nullable CodeReviewEditorGutterControlsState> myGutterState =
        MutableObservableValue.of(new AIReviewGutterState(Set.of(), Set.of()));
    private @Nullable AIReviewNewCommentInlay myNewComment;
    private @Nullable Disposable myNewCommentDisposable;
    private int myCounter;

    AIReviewModel(Project project, Document document, Runnable changed) {
        myProject = project;
        myDocument = document;
        myChanged = changed;
    }

    @Override
    public ObservableValue<Collection<CodeReviewInlayModel>> getInlays() {
        return myInlays;
    }

    @Override
    public ObservableValue<@Nullable CodeReviewEditorGutterControlsState> getGutterControlsState() {
        return myGutterState;
    }

    @RequiredUIAccess
    @Override
    public boolean canCreateComment(CodeReviewLineRange lineRange) {
        return lineRange.end() < Math.max(1, myDocument.getLineCount());
    }

    @RequiredUIAccess
    @Override
    public void requestNewComment(int lineIdx) {
        requestNewComment(new CodeReviewLineRange(lineIdx, lineIdx));
    }

    @RequiredUIAccess
    @Override
    public void requestNewComment(CodeReviewLineRange lineRange) {
        dropNewComment();
        Disposable disposable = Disposable.newDisposable();
        Disposer.register(this, disposable);
        AIReviewNewCommentViewModel editor = new AIReviewNewCommentViewModel(myProject, disposable, text -> {
            dropNewComment();
            addDraft(lineRange, text, new Date());
        });
        myNewComment = new AIReviewNewCommentInlay(lineRange, editor, () -> {
            dropNewComment();
            update();
        });
        myNewCommentDisposable = disposable;
        update();
        // the input exists once the editor drew the inlay - it takes the focus then
        UIAccess.current().give(editor::requestFocus);
    }

    @RequiredUIAccess
    @Override
    public void cancelNewComment(int lineIdx) {
        dropNewComment();
        update();
    }

    @RequiredUIAccess
    @Override
    public void toggleComments(int lineIdx) {
    }

    int getDraftCount() {
        return myDrafts.size();
    }

    /**
     * The drafts as they are kept between runs - the lines and the text of each.
     */
    List<AIPlanReviewDraftEntry> saveDrafts() {
        List<AIPlanReviewDraftEntry> entries = new ArrayList<>();
        for (AIReviewDraftInlay draft : myDrafts) {
            AIPlanReviewDraftEntry entry = new AIPlanReviewDraftEntry();
            entry.START = draft.getLines().start();
            entry.END = draft.getLines().end();
            entry.TEXT = draft.getComment().getText();
            Date createdAt = draft.getComment().getCreatedAt();
            entry.CREATED = createdAt == null ? 0 : createdAt.getTime();
            entries.add(entry);
        }
        return entries;
    }

    /**
     * Brings back the drafts of an earlier run - the ones on lines the plan no longer has are dropped.
     */
    void restoreDrafts(List<AIPlanReviewDraftEntry> entries) {
        int lineCount = Math.max(1, myDocument.getLineCount());
        for (AIPlanReviewDraftEntry entry : entries) {
            if (entry.START >= 0 && entry.START <= entry.END && entry.END < lineCount && entry.TEXT != null && !entry.TEXT.isBlank()) {
                addDraft(new CodeReviewLineRange(entry.START, entry.END), entry.TEXT, entry.CREATED > 0 ? new Date(entry.CREATED) : new Date());
            }
        }
    }

    /**
     * The drafts as the agent reads them - a line each, with the lines of the text it is about - and none of them after
     * it.
     */
    List<String> takeFeedback() {
        List<AIReviewDraftInlay> drafts = new ArrayList<>(myDrafts);
        drafts.sort((left, right) -> Integer.compare(left.getLines().start(), right.getLines().start()));
        List<String> feedback = new ArrayList<>();
        for (AIReviewDraftInlay draft : drafts) {
            CodeReviewLineRange lines = draft.getLines();
            String where = (lines.start() == lines.end()
                ? AIEditorLocalize.promptReviewLine(lines.start() + 1, quote(lines.start()))
                : AIEditorLocalize.promptReviewLines(lines.start() + 1, lines.end() + 1, quote(lines.start()))).get();
            feedback.add(where + " " + draft.getComment().getText());
        }
        myDrafts.clear();
        update();
        return feedback;
    }

    private String quote(int line) {
        if (line >= myDocument.getLineCount()) {
            return "";
        }
        String text = myDocument.getText().substring(myDocument.getLineStartOffset(line), myDocument.getLineEndOffset(line)).strip();
        return text.length() > MAX_QUOTE ? text.substring(0, MAX_QUOTE) + "…" : text;
    }

    private void addDraft(CodeReviewLineRange range, String text, Date createdAt) {
        String id = "plan-comment-" + (++myCounter);
        AIReviewDraftInlay[] draft = new AIReviewDraftInlay[1];
        draft[0] = new AIReviewDraftInlay(new AIReviewComment(id, text, createdAt, () -> {
            myDrafts.remove(draft[0]);
            update();
        }), range);
        myDrafts.add(draft[0]);
        update();
    }

    private void dropNewComment() {
        Disposable disposable = myNewCommentDisposable;
        myNewComment = null;
        myNewCommentDisposable = null;
        if (disposable != null) {
            Disposer.dispose(disposable);
        }
    }

    private void update() {
        List<CodeReviewInlayModel> inlays = new ArrayList<>(myDrafts);
        Set<Integer> commented = new LinkedHashSet<>();
        for (AIReviewDraftInlay draft : myDrafts) {
            commented.add(draft.getLines().end());
        }
        Set<Integer> newComments = new LinkedHashSet<>();
        AIReviewNewCommentInlay newComment = myNewComment;
        if (newComment != null) {
            inlays.add(newComment);
            newComments.add(newComment.getLines().end());
        }
        myInlays.set(inlays);
        myGutterState.set(new AIReviewGutterState(commented, newComments));
        myChanged.run();
    }

    @Override
    public void dispose() {
        myNewComment = null;
        myNewCommentDisposable = null;
    }
}
