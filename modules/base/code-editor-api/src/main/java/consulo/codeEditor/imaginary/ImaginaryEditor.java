/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.codeEditor.imaginary;

import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * This class is intended to simplify implementation of dummy editors needed only to pass to place which expect {@link Editor}
 * but do nothing complicate with it, only simple things like getting document/project/caret/selection.<p></p>
 * <p>
 * Since Imaginary* classes are intended to be used by multiple parties,
 * they should be as free as possible of simplified versions of any real Editor's logic.
 * Simplification involves making some assumptions what the clients would need, and different clients may disagree on that.
 * Having a simplified version that "almost always" works would make it hard to notice when it's not enough,
 * so the default implementation of most methods is to throw an exception to make the problem obvious immediately.
 * Clients can add simplified logic themselves via subclassing, if they really need to.
 *
 * @author VISTALL
 * @since 2025-03-07
 */
public class ImaginaryEditor extends UserDataHolderBase implements Editor {
    private static final Logger LOG = Logger.getInstance(ImaginaryEditor.class);

    private final Project myProject;
    private final Document myDocument;
    private final ImaginaryCaretModel myCaretModel;
    private final ImaginarySelectionModel mySelectionModel;

    private EditorColorsScheme myColorsScheme;
    private EditorHighlighter myHighlighter;
    private EditorSettings mySettings;
    private ImaginaryFoldingModel myFoldingModel;

    private JComponent myContentComponent;

    private EditorKind myEditorKind;

    public ImaginaryEditor(Project project, Document document) {
        myProject = project;
        myDocument = document;
        myCaretModel = new ImaginaryCaretModel(this);
        mySelectionModel = new ImaginarySelectionModel(this);
    }

    /**
     * Creates an ImaginaryEditor by capturing state from a real editor.
     * Captures caret position, selection, colors scheme, highlighter, and settings.
     */
    public static ImaginaryEditor create(Editor realEditor) {
        Document document = realEditor.getDocument();
        Project project = realEditor.getProject();
        ImaginaryEditor imaginary = new ImaginaryEditor(project, document);

        // Capture caret state
        Caret primaryCaret = realEditor.getCaretModel().getPrimaryCaret();
        imaginary.myCaretModel.getPrimaryCaret().moveToOffset(primaryCaret.getOffset());
        if (primaryCaret.hasSelection()) {
            ((ImaginaryCaret) imaginary.myCaretModel.getPrimaryCaret())
                .setSelection(primaryCaret.getSelectionStart(), primaryCaret.getSelectionEnd());
        }

        // Capture editor properties
        imaginary.myColorsScheme = realEditor.getColorsScheme();
        imaginary.myHighlighter = realEditor.getHighlighter();
        imaginary.mySettings = realEditor.getSettings();
        imaginary.myFoldingModel = ImaginaryFoldingModel.create(realEditor.getFoldingModel());
        imaginary.myContentComponent = realEditor.getContentComponent();
        imaginary.myEditorKind = realEditor.getEditorKind();
        return imaginary;
    }

    @Override
    public JComponent getContentComponent() {
        return myContentComponent;
    }

    protected RuntimeException notImplemented() {
        return new UnsupportedOperationException();
    }

    @Override
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public CaretModel getCaretModel() {
        return myCaretModel;
    }

    @Override
    public SelectionModel getSelectionModel() {
        return mySelectionModel;
    }

    @Override
    public ScrollingModel getScrollingModel() {
        return new ImaginaryScrollingModel(this);
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public boolean isInsertMode() {
        return true;
    }

    @Override
    public boolean isColumnMode() {
        return false;
    }

    @Override
    public EditorKind getEditorKind() {
        return myEditorKind;
    }

    @Override
    public int logicalPositionToOffset(LogicalPosition pos) {
        int lineCount = myDocument.getLineCount();
        if (lineCount == 0) {
            return 0;
        }
        int line = Math.max(0, Math.min(pos.line, lineCount - 1));
        int startOffset = myDocument.getLineStartOffset(line);
        int endOffset = myDocument.getLineEndOffset(line);
        return Math.max(startOffset, Math.min(startOffset + pos.column, endOffset));
    }

    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        int lineCount = myDocument.getLineCount();
        if (lineCount == 0) {
            return new LogicalPosition(0, 0);
        }
        int clamped = Math.max(0, Math.min(offset, myDocument.getTextLength()));
        int line = myDocument.getLineNumber(clamped);
        int col = clamped - myDocument.getLineStartOffset(line);
        return new LogicalPosition(line, col);
    }

    @Override
    public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
        return new VisualPosition(logicalPos.line, logicalPos.column);
    }

    @Override
    public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
        return new LogicalPosition(visiblePos.line, visiblePos.column);
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset) {
        LogicalPosition logicalPos = offsetToLogicalPosition(offset);
        return logicalToVisualPosition(logicalPos);
    }

    @Override
    public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
        return offsetToVisualPosition(offset);
    }

    @Override
    public EditorColorsScheme getColorsScheme() {
        if (myColorsScheme != null) {
            return myColorsScheme;
        }
        throw notImplemented();
    }

    @Override
    public EditorHighlighter getHighlighter() {
        if (myHighlighter != null) {
            return myHighlighter;
        }
        throw notImplemented();
    }

    @Override
    public EditorSettings getSettings() {
        if (mySettings != null) {
            return mySettings;
        }
        throw notImplemented();
    }

    // -- Unimplemented methods --

    @Override
    public boolean isViewer() {
        throw notImplemented();
    }

    @Override
    public boolean isOneLineMode() {
        throw notImplemented();
    }

    @Override
    public MarkupModel getMarkupModel() {
        throw notImplemented();
    }

    @Override
    public FoldingModel getFoldingModel() {
        if (myFoldingModel != null) {
            return myFoldingModel;
        }
        throw notImplemented();
    }

    @Override
    public SoftWrapModel getSoftWrapModel() {
        throw notImplemented();
    }

    @Override
    public EditorGutter getGutter() {
        throw notImplemented();
    }

    @Override
    public int getLineHeight() {
        throw notImplemented();
    }

    @Override
    public boolean hasHeaderComponent() {
        throw notImplemented();
    }

    @Override
    public IndentsModel getIndentsModel() {
        throw notImplemented();
    }

    @Override
    public InlayModel getInlayModel() {
        throw notImplemented();
    }

    @Override
    public DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(myContentComponent);
    }

    @Override
    public void addEditorMouseListener(EditorMouseListener listener) {
        LOG.info("Called ImaginaryEditor#addEditorMouseListener which is stubbed and has no implementation");
    }

    @Override
    public void removeEditorMouseListener(EditorMouseListener listener) {
        LOG.info("Called ImaginaryEditor#removeEditorMouseListener which is stubbed and has no implementation");
    }

    @Override
    public void addEditorMouseMotionListener(EditorMouseMotionListener listener) {
        throw notImplemented();
    }

    @Override
    public void removeEditorMouseMotionListener(EditorMouseMotionListener listener) {
        throw notImplemented();
    }
}
