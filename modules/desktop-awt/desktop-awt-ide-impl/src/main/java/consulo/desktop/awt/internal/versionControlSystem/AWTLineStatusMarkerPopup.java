/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.versionControlSystem;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.LogicalPosition;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.internal.DiffInternal;
import consulo.diff.internal.DiffLanguageUtil;
import consulo.diff.util.TextDiffType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.internal.EditorFragmentComponent;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.HintListener;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.LightweightHintFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.impl.internal.LineStatusMarkerPopupBase;
import consulo.versionControlSystem.impl.internal.LineStatusTracker;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getDiffType;

/**
 * The desktop look of the range popup: a {@link LightweightHint} carrying the toolbar over an
 * {@link EditorFragmentComponent} of the vcs lines.
 */
public class AWTLineStatusMarkerPopup extends LineStatusMarkerPopupBase {
    public AWTLineStatusMarkerPopup(LineStatusTracker tracker, Editor editor, VcsRange range) {
        super(tracker, editor, range);
    }

    @Override
    @RequiredUIAccess
    public void showHintAt(@Nullable InputDetails details) {
        if (!myTracker.isValid()) {
            return;
        }
        Disposable disposable = Disposable.newDisposable();

        FileType fileType = getFileType();
        List<DiffFragment> wordDiff = computeWordDiff();

        installMasterEditorHighlighters(wordDiff, disposable);
        JComponent editorComponent = createEditorComponent(fileType, wordDiff);

        Point mousePosition = toLayeredPanePoint(details);

        ActionGroup group = buildActionGroup(details, disposable);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group, true);
        toolbar.setTargetComponent(myEditor.getComponent());

        toolbar.updateActionsAsync().whenCompleteAsync((actions, throwable) -> {
            show(toolbar, mousePosition, disposable, editorComponent);
        }, UIAccess.current());
    }

    /**
     * The hint is placed in the layered pane, so a click reported against the gutter is carried over to it -
     * at the gutter's right edge, which is where the popup lines up with the text.
     */
    private @Nullable Point toLayeredPanePoint(@Nullable InputDetails details) {
        if (details == null) {
            return null;
        }

        JComponent gutterComponent = ((EditorEx)myEditor).getGutterComponentEx().getComponent();
        JRootPane rootPane = gutterComponent.getRootPane();
        if (rootPane == null) {
            return null;
        }

        return SwingUtilities.convertPoint(gutterComponent, gutterComponent.getWidth(), details.getY(), rootPane.getLayeredPane());
    }

    @RequiredUIAccess
    private void show(ActionToolbar toolbar, @Nullable Point mousePosition, Disposable disposable, JComponent editorComponent) {
        PopupPanel popupPanel = new PopupPanel(myEditor, toolbar, editorComponent);

        LightweightHint hint = Application.get().getInstance(LightweightHintFactory.class).create(popupPanel);
        HintListener closeListener = event -> Disposer.dispose(disposable);
        hint.addHintListener(closeListener);

        HintManagerEx hintManagerEx = (HintManagerEx)HintManager.getInstance();

        int line = myEditor.getCaretModel().getLogicalPosition().line;
        Point point = hintManagerEx.getHintPosition(hint, myEditor, new LogicalPosition(line, 0), HintManager.UNDER);
        if (mousePosition != null) { // show right after the nearest line
            int lineHeight = myEditor.getLineHeight();
            int delta = (point.y - mousePosition.y) % lineHeight;
            if (delta < 0) {
                delta += lineHeight;
            }
            point.y = mousePosition.y + delta;
        }
        point.x -= popupPanel.getEditorTextOffset(); // align main editor with the one in popup

        int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING;
        hintManagerEx.showEditorHint(hint, myEditor, point, flags, -1, false, new HintHint(myEditor.getContentComponent(), point));

        if (!hint.isVisible()) {
            closeListener.hintHidden(null);
        }
    }

    private @Nullable EditorFragmentComponent createEditorComponent(@Nullable FileType fileType, @Nullable List<DiffFragment> wordDiff) {
        if (myRange.getType() == VcsRange.INSERTED) {
            return null;
        }

        EditorEx uEditor = (EditorEx)EditorFactory.getInstance().createViewer(myTracker.getVcsDocument(), myTracker.getProject());
        uEditor.setColorsScheme(myEditor.getColorsScheme());

        DiffLanguageUtil.setEditorCodeStyle(myTracker.getProject(), uEditor, fileType);

        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        uEditor.setHighlighter(highlighterFactory.createEditorHighlighter(myTracker.getProject(), getFileName(myTracker.getDocument())));

        if (wordDiff != null) {
            DiffInternal diffInternal = Application.get().getInstance(DiffInternal.class);

            int vcsStartShift = myTracker.getVcsTextRange(myRange).getStartOffset();

            for (DiffFragment fragment : wordDiff) {
                int vcsStart = vcsStartShift + fragment.getStartOffset1();
                int vcsEnd = vcsStartShift + fragment.getEndOffset1();
                TextDiffType type = getDiffType(fragment);

                diffInternal.createInlineHighlighter(uEditor, vcsStart, vcsEnd, type);
            }
        }

        EditorFragmentComponent fragmentComponent =
            EditorFragmentComponent.createEditorFragmentComponent(uEditor, myRange.getVcsLine1(), myRange.getVcsLine2(), false, false);

        EditorFactory.getInstance().releaseEditor(uEditor);

        return fragmentComponent;
    }

    private static String getFileName(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return "";
        }
        return file.getName();
    }

    private static class PopupPanel extends JPanel {
        private final @Nullable JComponent myEditorComponent;

        public PopupPanel(final Editor editor, ActionToolbar toolbar, @Nullable JComponent editorComponent) {
            super(new BorderLayout());
            setOpaque(false);

            myEditorComponent = editorComponent;
            boolean isEditorVisible = myEditorComponent != null;

            Color background = TargetAWT.to(((EditorEx)editor).getBackgroundColor());
            Color borderColor = JBColor.border();

            JComponent toolbarComponent = toolbar.getComponent();
            toolbarComponent.setBackground(background);
            toolbarComponent.setBorder(null);

            JComponent toolbarPanel = JBUI.Panels.simplePanel(toolbarComponent);
            toolbarPanel.setBackground(background);
            Border outsideToolbarBorder = JBUI.Borders.customLine(borderColor, 1, 1, isEditorVisible ? 0 : 1, 1);
            Border insideToolbarBorder = JBUI.Borders.empty(1, 5, 1, 5);
            toolbarPanel.setBorder(BorderFactory.createCompoundBorder(outsideToolbarBorder, insideToolbarBorder));

            if (myEditorComponent != null) {
                // default border of EditorFragmentComponent is replaced here with our own.
                Border outsideEditorBorder = JBUI.Borders.customLine(borderColor, 1);
                Border insideEditorBorder = JBUI.Borders.empty(2);
                myEditorComponent.setBorder(BorderFactory.createCompoundBorder(outsideEditorBorder, insideEditorBorder));
            }

            // 'empty space' to the right of toolbar
            JPanel emptyPanel = new JPanel();
            emptyPanel.setOpaque(false);
            emptyPanel.setPreferredSize(new Dimension());

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            topPanel.add(toolbarPanel, BorderLayout.WEST);
            topPanel.add(emptyPanel, BorderLayout.CENTER);

            add(topPanel, BorderLayout.NORTH);
            if (myEditorComponent != null) {
                add(myEditorComponent, BorderLayout.CENTER);
            }

            // transfer clicks into editor
            MouseAdapter listener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    transferEvent(e, editor);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    transferEvent(e, editor);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    transferEvent(e, editor);
                }
            };
            emptyPanel.addMouseListener(listener);
        }

        private static void transferEvent(MouseEvent e, Editor editor) {
            editor.getContentComponent().dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, editor.getContentComponent()));
        }

        public int getEditorTextOffset() {
            return 3; // myEditorComponent.getInsets().left
        }
    }
}
