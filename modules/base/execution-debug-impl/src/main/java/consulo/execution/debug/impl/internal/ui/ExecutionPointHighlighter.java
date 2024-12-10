// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.ui;

import consulo.application.ApplicationManager;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.internal.EditorMouseHoverPopupControl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionWithHighlighter;
import consulo.execution.debug.impl.internal.XDebuggerUtilImpl;
import consulo.execution.debug.impl.internal.XSourcePositionImpl;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.ui.DebuggerColors;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author nik
 */
public class ExecutionPointHighlighter {
    private final Project myProject;
    private RangeHighlighter myRangeHighlighter;
    private Editor myEditor;
    private XSourcePosition mySourcePosition;
    private OpenFileDescriptor myOpenFileDescriptor;
    private boolean myNotTopFrame;
    private GutterIconRenderer myGutterIconRenderer;
    public static final Key<Boolean> EXECUTION_POINT_HIGHLIGHTER_TOP_FRAME_KEY = Key.create("EXECUTION_POINT_HIGHLIGHTER_TOP_FRAME_KEY");

    private final AtomicBoolean updateRequested = new AtomicBoolean();

    public ExecutionPointHighlighter(@Nonnull Project project) {
        myProject = project;

        // Update highlighter colors if global color schema was changed
        project.getMessageBus().connect().subscribe(EditorColorsListener.class, scheme -> update(false));
    }

    public void show(final @Nonnull XSourcePosition position, final boolean notTopFrame, @Nullable final GutterIconRenderer gutterIconRenderer) {
        updateRequested.set(false);
        myProject.getUIAccess().give(() -> {
            updateRequested.set(false);

            mySourcePosition = position;

            clearDescriptor();
            myOpenFileDescriptor = findOpenFileDescriptor(myProject, position);

            if (!XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isScrollToCenter()) {
                myOpenFileDescriptor.putUserData(ScrollType.KEY, notTopFrame ? ScrollType.CENTER : ScrollType.MAKE_VISIBLE);
            }
            //see IDEA-125645 and IDEA-63459
            //myOpenFileDescriptor.setUseCurrentWindow(true);

            myGutterIconRenderer = gutterIconRenderer;
            myNotTopFrame = notTopFrame;

            doShow(true);
        });
    }

    @Nonnull
    private OpenFileDescriptor findOpenFileDescriptor(@Nonnull Project project, @Nonnull XSourcePosition position) {
        Navigatable navigatable = position.createNavigatable(project);
        if (navigatable instanceof OpenFileDescriptor openFileDescriptor) {
            return openFileDescriptor;
        }
        else {
            return XSourcePositionImpl.createOpenFileDescriptor(project, position);
        }
    }

    public void hide() {
        AppUIUtil.invokeOnEdt(() -> {
            updateRequested.set(false);

            removeHighlighter();
            clearDescriptor();
            myEditor = null;
            myGutterIconRenderer = null;
        });
    }

    private void clearDescriptor() {
        if (myOpenFileDescriptor != null) {
            myOpenFileDescriptor.dispose();
            myOpenFileDescriptor = null;
        }
    }

    public void navigateTo() {
        if (myOpenFileDescriptor != null && myOpenFileDescriptor.getFile().isValid()) {
            myOpenFileDescriptor.navigateInEditor(true);
        }
    }

    @Nullable
    public VirtualFile getCurrentFile() {
        return myOpenFileDescriptor != null ? myOpenFileDescriptor.getFile() : null;
    }

    public void update(final boolean navigate) {
        if (updateRequested.compareAndSet(false, true)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (updateRequested.compareAndSet(true, false)) {
                    doShow(navigate);
                }
            }, myProject.getDisposed());
        }
    }

    public void updateGutterIcon(@Nullable final GutterIconRenderer renderer) {
        AppUIUtil.invokeOnEdt(() -> {
            if (myRangeHighlighter != null && myGutterIconRenderer != null) {
                myRangeHighlighter.setGutterIconRenderer(renderer);
            }
        });
    }

    private void doShow(boolean navigate) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        removeHighlighter();

        OpenFileDescriptor fileDescriptor = myOpenFileDescriptor;
        if (!navigate && myOpenFileDescriptor != null) {
            fileDescriptor = OpenFileDescriptorFactory.getInstance(myProject).newBuilder(myOpenFileDescriptor.getFile()).build();
        }

        myEditor = null;

        if (fileDescriptor != null) {
            if (!navigate) {
                FileEditor editor = FileEditorManager.getInstance((Project) fileDescriptor.getProject()).getSelectedEditor(fileDescriptor.getFile());
                if (editor instanceof TextEditor) {
                    myEditor = ((TextEditor) editor).getEditor();
                }
            }
            if (myEditor == null) {
                myEditor = XDebuggerUtilImpl.createEditor(fileDescriptor);
            }
        }
        if (myEditor != null) {
            addHighlighter();
        }
    }

    private void removeHighlighter() {
        if (myEditor != null) {
            disableMouseHoverPopups(myEditor, false);
        }

        //if (myNotTopFrame && myEditor != null) {
        //  myEditor.getSelectionModel().removeSelection();
        //}

        if (myRangeHighlighter != null) {
            myRangeHighlighter.dispose();
            myRangeHighlighter = null;
        }
    }

    private void addHighlighter() {
        disableMouseHoverPopups(myEditor, true);
        int line = mySourcePosition.getLine();
        Document document = myEditor.getDocument();
        if (line < 0 || line >= document.getLineCount()) {
            return;
        }

        //if (myNotTopFrame) {
        //  myEditor.getSelectionModel().setSelection(document.getLineStartOffset(line), document.getLineEndOffset(line) + document.getLineSeparatorLength(line));
        //  return;
        //}

        if (myRangeHighlighter != null) {
            return;
        }

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        TextAttributes attributes = myNotTopFrame ? scheme.getAttributes(DebuggerColors.NOT_TOP_FRAME_ATTRIBUTES) : scheme.getAttributes(DebuggerColors.EXECUTIONPOINT_ATTRIBUTES);
        MarkupModel markupModel = DocumentMarkupModel.forDocument(document, myProject, true);
        if (mySourcePosition instanceof XSourcePositionWithHighlighter) {
            TextRange range = ((XSourcePositionWithHighlighter) mySourcePosition).getHighlightRange();
            if (range != null) {
                TextRange lineRange = DocumentUtil.getLineTextRange(document, line);
                if (!range.equals(lineRange)) {
                    myRangeHighlighter = markupModel.addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), DebuggerColors.EXECUTION_LINE_HIGHLIGHTERLAYER, attributes, HighlighterTargetArea.EXACT_RANGE);
                }
            }
        }
        if (myRangeHighlighter == null) {
            myRangeHighlighter = markupModel.addLineHighlighter(line, DebuggerColors.EXECUTION_LINE_HIGHLIGHTERLAYER, attributes);
        }
        myRangeHighlighter.putUserData(EXECUTION_POINT_HIGHLIGHTER_TOP_FRAME_KEY, !myNotTopFrame);
        myRangeHighlighter.setGutterIconRenderer(myGutterIconRenderer);
    }

    public boolean isFullLineHighlighter() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        return myRangeHighlighter != null && myRangeHighlighter.getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE;
    }

    private static void disableMouseHoverPopups(@Nonnull final Editor editor, final boolean disable) {
        Project project = editor.getProject();
        if (ApplicationManager.getApplication().isUnitTestMode() || project == null) {
            return;
        }

        // need to always invoke later to maintain order of enabling/disabling
        SwingUtilities.invokeLater(() -> {
            if (disable) {
                EditorMouseHoverPopupControl.disablePopups(project);
            }
            else {
                EditorMouseHoverPopupControl.enablePopups(project);
            }
        });
    }
}
