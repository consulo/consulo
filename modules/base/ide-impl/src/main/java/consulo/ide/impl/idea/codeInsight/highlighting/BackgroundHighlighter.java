// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.event.*;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.ide.impl.idea.find.impl.livePreview.LivePreviewController;
import consulo.language.editor.template.TemplateManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class BackgroundHighlighter implements PostStartupActivity {
    public static final Key<SelectionHighlights> SELECTION_HIGHLIGHTS = SelectionHighlights.KEY;

    private final EarlyAccessProgramManager myEarlyAccessProgramManager;

    @Inject
    public BackgroundHighlighter(EarlyAccessProgramManager earlyAccessProgramManager) {
        myEarlyAccessProgramManager = earlyAccessProgramManager;
    }

    @Override
    public void runActivity(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
        final EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();

        Alarm braceAlarm = new Alarm();

        Alarm selectionAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);

        eventMulticaster.addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@Nonnull CaretEvent e) {
                if (e.getCaret() != e.getEditor().getCaretModel().getPrimaryCaret()) {
                    return;
                }
                braceAlarm.cancelAllRequests();
                Editor editor = e.getEditor();
                final SelectionModel selectionModel = editor.getSelectionModel();
                // Don't update braces in case of the active selection.
                if (editor.getProject() != project || selectionModel.hasSelection()) {
                    return;
                }
                updateBraces(editor, braceAlarm);


                if (!highlightSelection(project, editor, selectionAlarm)) {
                    removeSelectionHighlights(editor);
                }
            }
        }, project);

        final SelectionListener selectionListener = new SelectionListener() {
            @Override
            @RequiredUIAccess
            public void selectionChanged(@Nonnull SelectionEvent e) {
                braceAlarm.cancelAllRequests();
                Editor editor = e.getEditor();
                if (editor.getProject() != project) {
                    return;
                }

                if (!highlightSelection(project, editor, selectionAlarm)) {
                    removeSelectionHighlights(editor);
                }

                final TextRange oldRange = e.getOldRange();
                final TextRange newRange = e.getNewRange();
                if (oldRange != null && newRange != null && oldRange.isEmpty() == newRange.isEmpty()) {
                    // Don't perform braces update in case of active/absent selection.
                    return;
                }
                updateBraces(editor, braceAlarm);
            }
        };
        eventMulticaster.addSelectionListener(selectionListener, project);

        DocumentListener documentListener = new DocumentListener() {
            @Override
            @RequiredUIAccess
            public void documentChanged(@Nonnull DocumentEvent e) {
                braceAlarm.cancelAllRequests();
                Editor[] editors = EditorFactory.getInstance().getEditors(e.getDocument(), project);
                for (Editor editor : editors) {
                    updateBraces(editor, braceAlarm);

                    if (!highlightSelection(project, editor, selectionAlarm)) {
                        removeSelectionHighlights(editor);
                    }
                }
            }
        };
        eventMulticaster.addDocumentListener(documentListener, project);

        project.getMessageBus().connect().subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            @RequiredUIAccess
            public void selectionChanged(@Nonnull FileEditorManagerEvent e) {
                braceAlarm.cancelAllRequests();

                FileEditor oldEditor = e.getOldEditor();
                if (oldEditor instanceof TextEditor) {
                    clearBraces(((TextEditor) oldEditor).getEditor(), braceAlarm);
                }

                FileEditor newEditor = e.getNewEditor();
                if (newEditor instanceof TextEditor) {
                    Editor editor = ((TextEditor) newEditor).getEditor();

                    updateBraces(editor, braceAlarm);

                    if (!highlightSelection(project, editor, selectionAlarm)) {
                        removeSelectionHighlights(editor);
                    }
                }
            }
        });
    }

    @RequiredUIAccess
    private boolean highlightSelection(Project project, Editor editor, Alarm alarm) {
        UIAccess.assertIsUIThread();

        if (!myEarlyAccessProgramManager.getState(HighlightSelectionEarlyAccessProgramDescriptor.class)) {
            return false;
        }

        Document document = editor.getDocument();
        final long stamp = document.getModificationStamp();
        if (document.isInBulkUpdate() || !BraceHighlightingHandler.isValidEditor(editor)) {
            return false;
        }
        if (!editor.getSettings().isHighlightSelectionOccurrences()) {
            return false;
        }
        if (TemplateManager.getInstance(project).getTemplateState(editor) != null) {
            return false;
        }
        CaretModel caretModel = editor.getCaretModel();
        if (caretModel.getCaretCount() > 1) {
            return false;
        }
        Caret caret = caretModel.getPrimaryCaret();
        if (!caret.hasSelection()) {
            return false;
        }
        final int start = caret.getSelectionStart();
        final int end = caret.getSelectionEnd();
        CharSequence sequence = document.getCharsSequence();
        final String toFind = sequence.subSequence(start, end).toString();
        if (toFind.isBlank() || toFind.contains("\n")) {
            return false;
        }
        SelectionHighlights previous = editor.getUserData(SELECTION_HIGHLIGHTS);
        if (previous != null && toFind.equals(previous.text())) {
            return true;
        }
        FindManager findManager = FindManager.getInstance(project);
        final FindModel findModel = new FindModel();
        findModel.copyFrom(findManager.getFindInFileModel());
        findModel.setRegularExpressions(false);
        findModel.setStringToFind(toFind);
        final int threshold = Registry.intValue("editor.highlight.selected.text.max.occurrences.threshold", 50);

        ReadAction.nonBlocking(() -> {
                if (!BraceHighlightingHandler.isValidEditor(editor) || !caret.hasSelection()) {
                    return Collections.<FindResult>emptyList();
                }
                FindResult result = findManager.findString(sequence, 0, findModel, null);
                List<FindResult> results = new ArrayList<>();
                int count = 0;
                while (result.isStringFound() && count < LivePreviewController.MATCHES_LIMIT) {
                    count++;
                    if (count > threshold) {
                        return Collections.<FindResult>emptyList();
                    }
                    results.add(result);
                    result = findManager.findString(sequence, result.getEndOffset(), findModel);
                }
                return results;
            })
            .coalesceBy(HighlightSelectionKey.class, editor)
            .expireWhen(() -> document.getModificationStamp() != stamp || !BraceHighlightingHandler.isValidEditor(editor))
            .finishOnUiThread(Application::getNoneModalityState, results -> {
                if (!BraceHighlightingHandler.isValidEditor(editor)) {
                    return;
                }
                removeSelectionHighlights(editor);
                if (document.getModificationStamp() != stamp || results.isEmpty()) {
                    return;
                }
                List<RangeHighlighter> highlighters = new ArrayList<>();
                MarkupModel markupModel = editor.getMarkupModel();
                for (FindResult result : results) {
                    int startOffset = result.getStartOffset();
                    int endOffset = result.getEndOffset();
                    if (startOffset == start && endOffset == end) {
                        continue;
                    }
                    highlighters.add(
                        markupModel.addRangeHighlighter(
                            EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES,
                            startOffset,
                            endOffset,
                            HighlightManagerImpl.OCCURRENCE_LAYER,
                            HighlighterTargetArea.EXACT_RANGE
                        )
                    );
                }
                editor.putUserData(SELECTION_HIGHLIGHTS, new SelectionHighlights(toFind, highlighters));
            })
            .submit(command -> alarm.addRequest(command, 0, project.getApplication().getNoneModalityState()));
        return true;
    }

    private static void removeSelectionHighlights(Editor editor) {
        SelectionHighlights highlights = editor.getUserData(SELECTION_HIGHLIGHTS);
        if (highlights == null) {
            return;
        }
        editor.putUserData(SELECTION_HIGHLIGHTS, null);
        MarkupModel markupModel = editor.getMarkupModel();
        for (RangeHighlighter highlighter : highlights.highlighters()) {
            markupModel.removeHighlighter(highlighter);
        }
    }

    static void updateBraces(@Nonnull final Editor editor, @Nonnull final Alarm alarm) {
        if (editor.getDocument().isInBulkUpdate()) {
            return;
        }

        BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, alarm, handler -> {
            handler.updateBraces();
            return false;
        });
    }

    private void clearBraces(@Nonnull final Editor editor, @Nonnull Alarm braceAlarm) {
        BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, braceAlarm, handler -> {
            handler.clearBraceHighlighters();
            return false;
        });
    }
}
