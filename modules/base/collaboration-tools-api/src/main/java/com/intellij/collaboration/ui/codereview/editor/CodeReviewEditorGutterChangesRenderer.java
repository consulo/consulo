// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.application.AllIcons;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.colorScheme.TextAttributes;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.util.DiffUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.moveUpDown.LineRange;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.lang.Range;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineName;
import kotlinx.coroutines.Dispatchers;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Draws and handles review changes markers in gutter
 */
@ApiStatus.NonExtendable
public class CodeReviewEditorGutterChangesRenderer extends LineStatusMarkerRendererWithPopup {

    protected final CodeReviewEditorGutterActionableChangesModel model;
    protected final Editor editor;
    private final LineStatusMarkerColorScheme lineStatusMarkerColorScheme;

    public CodeReviewEditorGutterChangesRenderer(
        @Nonnull CodeReviewEditorGutterActionableChangesModel model,
        @Nonnull Editor editor,
        @Nonnull Disposable disposable,
        @Nonnull LineStatusMarkerColorScheme lineStatusMarkerColorScheme
    ) {
        super(editor.getProject(), editor.getDocument(), model, disposable, e -> e == editor);
        this.model = model;
        this.editor = editor;
        this.lineStatusMarkerColorScheme = lineStatusMarkerColorScheme;
    }

    public CodeReviewEditorGutterChangesRenderer(
        @Nonnull CodeReviewEditorGutterActionableChangesModel model,
        @Nonnull Editor editor,
        @Nonnull Disposable disposable
    ) {
        this(model, editor, disposable, ReviewInEditorUtil.REVIEW_STATUS_MARKER_COLOR_SCHEME);
    }

    @Override
    protected void paintGutterMarkers(@Nonnull Editor editor, @Nonnull List<? extends Range> ranges, @Nonnull Graphics g) {
        LineStatusMarkerDrawUtil.paintDefault(
            editor,
            g,
            ranges,
            DefaultFlagsProvider.DEFAULT,
            lineStatusMarkerColorScheme, 0
        );
    }

    @Override
    protected @Nonnull TextAttributes createErrorStripeTextAttributes(byte diffType) {
        return new ReviewChangesTextAttributes();
    }

    private class ReviewChangesTextAttributes extends TextAttributes {
        @Override
        public Color getErrorStripeColor() {
            return ReviewInEditorUtil.REVIEW_CHANGES_STATUS_COLOR;
        }
    }

    @Override
    @RequiredUIAccess
    protected @Nonnull LineStatusMarkerPopupPanel createPopupPanel(
        @Nonnull Editor editor,
        @Nonnull Range range,
        @Nullable Point mousePosition,
        @Nonnull Disposable disposable
    ) {
        String vcsContent = model.getBaseContent(new LineRange(range.getVcsLine1(), range.getVcsLine2()));
        if (vcsContent != null && vcsContent.endsWith("\n")) {
            vcsContent = vcsContent.substring(0, vcsContent.length() - 1);
        }

        JComponent editorComponent;
        if (vcsContent != null) {
            Editor popupEditor = createPopupEditor(getProject(), editor, vcsContent, disposable);
            showLineDiff(editor, popupEditor, range, vcsContent, disposable);
            editorComponent = LineStatusMarkerPopupPanel.createEditorComponent(editor, popupEditor.getComponent());
        }
        else {
            editorComponent = null;
        }

        List<AnAction> actions = createActions(range);
        JComponent toolbar = LineStatusMarkerPopupPanel.buildToolbar(editor, actions, disposable);
        return LineStatusMarkerPopupPanel.create(editor, toolbar, editorComponent, null);
    }

    protected @Nonnull List<AnAction> createActions(@Nonnull Range range) {
        return List.of(
            new ShowPrevChangeMarkerAction(range),
            new ShowNextChangeMarkerAction(range),
            new CopyLineStatusRangeAction(range),
            new ShowDiffAction(range),
            new ToggleByWordDiffAction()
        );
    }

    private @Nonnull Editor createPopupEditor(
        @Nullable Project project,
        @Nonnull Editor mainEditor,
        @Nonnull String vcsContent,
        @Nonnull Disposable disposable
    ) {
        EditorFactory factory = EditorFactory.getInstance();
        EditorEx popupEditor = (EditorEx) factory.createViewer(factory.createDocument(vcsContent), project, EditorKind.DIFF);

        ReadAction.run(() -> {
            popupEditor.setCaretEnabled(false);
            popupEditor.getContentComponent().setFocusCycleRoot(false);

            popupEditor.setRendererMode(true);
            EditorTextField.setupTextFieldEditor(popupEditor);
            popupEditor.setVerticalScrollbarVisible(true);
            popupEditor.setHorizontalScrollbarVisible(true);
            popupEditor.setBorder(null);

            popupEditor.getSettings().setUseSoftWraps(false);
            popupEditor.getSettings().setTabSize(mainEditor.getSettings().getTabSize(project));
            popupEditor.getSettings().setUseTabCharacter(mainEditor.getSettings().isUseTabCharacter(project));

            popupEditor.setColorsScheme(mainEditor.getColorsScheme());
            popupEditor.setBackgroundColor(LineStatusMarkerPopupPanel.getEditorBackgroundColor(mainEditor));

            popupEditor.getSelectionModel().removeSelection();
        });

        WhenDisposedKt.whenDisposed(
            disposable,
            () -> {
                factory.releaseEditor(popupEditor);
                return Unit.INSTANCE;
            }
        );

        return popupEditor;
    }

    @RequiredUIAccess
    private void showLineDiff(
        @Nonnull Editor editor,
        @Nonnull Editor popupEditor,
        @Nonnull Range range,
        @Nonnull CharSequence vcsContent,
        @Nonnull Disposable disposable
    ) {
        Disposable[] highlightersDisposable = {null};

        Runnable update = () -> {
            boolean show = model.getShouldHighlightDiffRanges();
            if (show && highlightersDisposable[0] == null) {
                CharSequence currentContent = DiffUtil.getLinesContent(editor.getDocument(), range.getLine1(), range.getLine2());
                if (currentContent.length() == 0) {
                    return;
                }

                Disposable newDisposable = Disposer.newDisposable();
                Disposer.register(disposable, newDisposable);
                highlightersDisposable[0] = newDisposable;

                var lineDiff = BackgroundTaskUtil.tryComputeFast(
                    (ProgressIndicator indicator) -> ComparisonManager.getInstance().compareLines(
                        vcsContent,
                        currentContent,
                        ComparisonPolicy.DEFAULT,
                        indicator
                    ),
                    200
                );
                if (lineDiff == null) {
                    return;
                }

                LineStatusMarkerPopupPanel.installMasterEditorWordHighlighters(
                    editor,
                    range.getLine1(),
                    range.getLine2(),
                    lineDiff,
                    newDisposable
                );
                List<RangeHighlighter> highlighters = LineStatusMarkerPopupPanel.installEditorDiffHighlighters(popupEditor, lineDiff);
                WhenDisposedKt.whenDisposed(
                    newDisposable,
                    () -> {
                        for (RangeHighlighter h : highlighters) {
                            h.dispose();
                        }
                        return Unit.INSTANCE;
                    }
                );
            }
            else if (!show && highlightersDisposable[0] != null) {
                Disposer.dispose(highlightersDisposable[0]);
                highlightersDisposable[0] = null;
            }
        };

        model.addDiffHighlightListener(disposable, update);
        update.run();
    }

    protected class ShowNextChangeMarkerAction extends LineStatusMarkerPopupActions.RangeMarkerAction implements LightEditCompatible {
        public ShowNextChangeMarkerAction(@Nonnull Range range) {
            super(CodeReviewEditorGutterChangesRenderer.this.editor, getRangesSource(), range, "VcsShowNextChangeMarker");
        }

        @Override
        protected boolean isEnabled(@Nonnull Editor editor, @Nonnull Range range) {
            return getNextRangeInternal(range.getLine1()) != null;
        }

        @Override
        protected void actionPerformed(@Nonnull Editor editor, @Nonnull Range range) {
            Range targetRange = getNextRangeInternal(range.getLine1());
            if (targetRange != null) {
                scrollAndShow(editor, targetRange);
            }
        }

        private @Nullable Range getNextRangeInternal(int line) {
            List<? extends Range> ranges = getRangesSource().getRanges();
            if (ranges == null) {
                return null;
            }
            return CodeReviewEditorGutterChangesRenderer.getNextRange(ranges, line);
        }
    }

    protected class ShowPrevChangeMarkerAction extends LineStatusMarkerPopupActions.RangeMarkerAction implements LightEditCompatible {
        public ShowPrevChangeMarkerAction(@Nonnull Range range) {
            super(CodeReviewEditorGutterChangesRenderer.this.editor, getRangesSource(), range, "VcsShowPrevChangeMarker");
        }

        @Override
        protected boolean isEnabled(@Nonnull Editor editor, @Nonnull Range range) {
            return getPrevRange(range.getLine1()) != null;
        }

        @Override
        protected void actionPerformed(@Nonnull Editor editor, @Nonnull Range range) {
            Range targetRange = getPrevRange(range.getLine1());
            if (targetRange != null) {
                scrollAndShow(editor, targetRange);
            }
        }

        private @Nullable Range getPrevRange(int line) {
            List<? extends Range> ranges = getRangesSource().getRanges();
            if (ranges == null) {
                return null;
            }
            List<? extends Range> reversed = ranges.reversed();
            return CodeReviewEditorGutterChangesRenderer.getNextRange(reversed, line);
        }
    }

    protected class CopyLineStatusRangeAction extends LineStatusMarkerPopupActions.RangeMarkerAction implements LightEditCompatible {
        public CopyLineStatusRangeAction(@Nonnull Range range) {
            super(CodeReviewEditorGutterChangesRenderer.this.editor, getRangesSource(), range, IdeActions.ACTION_COPY);
        }

        @Override
        protected boolean isEnabled(@Nonnull Editor editor, @Nonnull Range range) {
            return range.hasVcsLines();
        }

        @Override
        protected void actionPerformed(@Nonnull Editor editor, @Nonnull Range range) {
            String content = model.getBaseContent(new LineRange(range.getVcsLine1(), range.getVcsLine2()));
            CopyPasteManager.getInstance().setContents(new StringSelection(content));
        }
    }

    protected class ShowDiffAction extends LineStatusMarkerPopupActions.RangeMarkerAction implements LightEditCompatible {
        public ShowDiffAction(@Nonnull Range range) {
            super(CodeReviewEditorGutterChangesRenderer.this.editor, getRangesSource(), range, "Vcs.ShowDiffChangedLines");
            setShortcutSet(new CompositeShortcutSet(
                KeymapUtil.getActiveKeymapShortcuts("Vcs.ShowDiffChangedLines"),
                KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_DIFF_COMMON)
            ));
            getTemplatePresentation().setTextValue(CollaborationToolsLocalize.reviewDiffActionShowText());
            getTemplatePresentation().setDescriptionValue(CollaborationToolsLocalize.reviewDiffActionShowDescription());
        }

        @Override
        protected boolean isEnabled(@Nonnull Editor editor, @Nonnull Range range) {
            return true;
        }

        @Override
        protected void actionPerformed(@Nonnull Editor editor, @Nonnull Range range) {
            model.showDiff(range.getLine1());
        }
    }

    protected class ToggleByWordDiffAction extends ToggleAction implements DumbAware, LightEditCompatible {
        public ToggleByWordDiffAction() {
            super(CollaborationToolsLocalize.reviewEditorActionHighlightLinesText().get(), null, AllIcons.Actions.Highlighting);
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return model.getShouldHighlightDiffRanges();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            model.setShouldHighlightDiffRanges(state);
        }
    }

    private static @Nullable Range getNextRange(@Nonnull List<? extends Range> ranges, int line) {
        boolean found = false;
        for (Range range : ranges) {
            if (DiffUtil.isSelectedByLine(line, range.getLine1(), range.getLine2())) {
                found = true;
            }
            else if (found) {
                return range;
            }
        }
        return null;
    }

    /**
     * Suspending render method - kept as a static helper for Kotlin coroutine callers.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object render(
        @Nonnull CodeReviewEditorGutterActionableChangesModel model,
        @Nonnull Editor editor,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        return kotlinx.coroutines.BuildersKt.withContext(
            kotlinx.coroutines.Dispatchers.getMain().immediate().plus(new CoroutineName("Editor gutter code review changes renderer")),
            (scope, cont) -> {
                Disposable disposable = Disposer.newDisposable("Editor code review changes renderer disposable");
                editor.putUserData(CodeReviewEditorGutterActionableChangesModel.KEY, model);
                try {
                    CodeReviewEditorGutterChangesRenderer renderer = new CodeReviewEditorGutterChangesRenderer(
                        model, editor, disposable, ReviewInEditorUtil.REVIEW_STATUS_MARKER_COLOR_SCHEME
                    );
                    // Collect reviewRanges and scheduleUpdate on each emission
                    return kotlinx.coroutines.flow.FlowKt.collect(
                        model.getReviewRanges(),
                        value -> {
                            renderer.scheduleUpdate();
                            return Unit.INSTANCE;
                        },
                        cont
                    );
                }
                finally {
                    Disposer.dispose(disposable);
                    editor.putUserData(CodeReviewEditorGutterActionableChangesModel.KEY, null);
                }
            },
            continuation
        );
    }
}
