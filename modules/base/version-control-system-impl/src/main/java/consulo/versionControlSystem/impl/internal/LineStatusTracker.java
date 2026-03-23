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
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getLineCount;

/**
 * @author irengrig
 * @author lesya
 */
@SuppressWarnings({"MethodMayBeStatic", "FieldAccessedSynchronizedAndUnsynchronized"})
public class LineStatusTracker extends LineStatusTrackerBase {
    public enum Mode {
        DEFAULT,
        SMART,
        SILENT
    }

    private static final Key<JComponent> PANEL_KEY = Key.create("LineStatusTracker.CanNotCalculateDiffPanel");


    private final VirtualFile myVirtualFile;


    private final FileEditorManager myFileEditorManager;

    private final VcsDirtyScopeManager myVcsDirtyScopeManager;


    private Mode myMode;

    /**
     * Document-level gutter renderer: ONE highlighter for the whole file, paints all
     * changed-line markers at repaint time, debounced via {@link consulo.ui.ex.awt.util.MergingUpdateQueue}.
     * Created once, lives until {@link #release()}.
     */
    private @Nullable LineStatusGutterMarkerRenderer myGutterRenderer;

    private LineStatusTracker(Project project,
                              Document document,
                              VirtualFile virtualFile,
                              Mode mode) {
        super(project, document);
        myVirtualFile = virtualFile;
        myMode = mode;

        myFileEditorManager = FileEditorManager.getInstance(project);
        myVcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
    }

    public static LineStatusTracker createOn(VirtualFile virtualFile,
                                             Document document,
                                             Project project,
                                             Mode mode) {
        LineStatusTracker tracker = new LineStatusTracker(project, document, virtualFile, mode);
        // Create document-level renderer only for non-silent mode; SILENT trackers never paint.
        if (mode != Mode.SILENT) {
            tracker.myGutterRenderer = new LineStatusGutterMarkerRenderer(tracker);
        }
        return tracker;
    }

    
    @Override
    public Project getProject() {
        //noinspection ConstantConditions
        return super.getProject();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    
    @RequiredUIAccess
    public Mode getMode() {
        return myMode;
    }

    @RequiredUIAccess
    public boolean isSilentMode() {
        return myMode == Mode.SILENT;
    }

    @RequiredUIAccess
    public void setMode(Mode mode) {
        if (myMode == mode) {
            return;
        }

        boolean wasSilent = myMode == Mode.SILENT;
        boolean nowSilent = mode == Mode.SILENT;
        myMode = mode;

        if (wasSilent && !nowSilent) {
            // Switching FROM silent: create the document-level gutter renderer.
            myGutterRenderer = new LineStatusGutterMarkerRenderer(this);
        }
        else if (!wasSilent && nowSilent) {
            // Switching TO silent: dispose the gutter renderer.
            LineStatusGutterMarkerRenderer renderer = myGutterRenderer;
            myGutterRenderer = null;
            if (renderer != null) {
                renderer.dispose();
            }
        }

        reinstallRanges();
    }

    @Override
    @RequiredUIAccess
    protected boolean isDetectWhitespaceChangedLines() {
        return myMode == Mode.SMART;
    }

    @Override
    @RequiredUIAccess
    protected void installNotification(String text) {
        FileEditor[] editors = myFileEditorManager.getAllEditors(myVirtualFile);
        for (FileEditor editor : editors) {
            JComponent panel = editor.getUserData(PANEL_KEY);
            if (panel == null) {
                EditorNotificationBuilderFactory factory = Application.get().getInstance(EditorNotificationBuilderFactory.class);

                EditorNotificationBuilderEx builder = (EditorNotificationBuilderEx) factory.newBuilder();
                builder.withText(LocalizeValue.ofNullable(text));

                JComponent newPanel = builder.getComponent();
                editor.putUserData(PANEL_KEY, newPanel);
                myFileEditorManager.addTopComponent(editor, newPanel);
            }
        }
    }

    @Override
    @RequiredUIAccess
    protected void destroyNotification() {
        FileEditor[] editors = myFileEditorManager.getEditors(myVirtualFile);
        for (FileEditor editor : editors) {
            JComponent panel = editor.getUserData(PANEL_KEY);
            if (panel != null) {
                myFileEditorManager.removeTopComponent(editor, panel);
                editor.putUserData(PANEL_KEY, null);
            }
        }
    }

    @Override
    @RequiredUIAccess
    protected void createHighlighter(VcsRange range) {
        UIAccess.assertIsUIThread();

        if (range.getHighlighter() != null) {
            LineStatusTrackerBase.LOG.error("Multiple highlighters registered for the same Range");
            return;
        }

        if (myMode == Mode.SILENT) {
            return;
        }

        int first =
            range.getLine1() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine1());
        int second =
            range.getLine2() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine2());

        MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, true);

        // Create a range highlighter for the error stripe (scrollbar colored dots) only.
        // Gutter painting is delegated to the document-level LineStatusGutterMarkerRenderer,
        // so no LineMarkerRenderer is attached here.
        RangeHighlighter highlighter = LineStatusMarkerRenderer.createRangeHighlighter(range, new TextRange(first, second), markupModel);

        range.setHighlighter(highlighter);
    }

    @Override
    @RequiredUIAccess
    protected void fireFileUnchanged() {
        // later to avoid saving inside document change event processing.
        FileDocumentManager.getInstance().saveDocument(myDocument);
        List<VcsRange> ranges = getRanges();
        if (ranges == null || ranges.isEmpty()) {
            // file was modified, and now it's not -> dirty local change
            myVcsDirtyScopeManager.fileDirty(myVirtualFile);
        }
    }

    @Override
    public void release() {
        // RangeHighlighter.dispose() requires the EDT.
        // Mirror the same pattern as LineStatusTrackerBase.release() so renderer
        // disposal is always scheduled on the dispatch thread even when release()
        // is called from a background thread (e.g. BaseRevisionLoader).
        LineStatusGutterMarkerRenderer renderer = myGutterRenderer;
        myGutterRenderer = null;
        if (renderer != null) {
            if (myApplication.isDispatchThread()) {
                renderer.dispose();
            }
            else {
                myApplication.invokeLater(renderer::dispose);
            }
        }
        super.release();
    }

    @Override
    protected void doRollbackRange(VcsRange range) {
        super.doRollbackRange(range);
        markLinesUnchanged(range.getLine1(), range.getLine1() + range.getVcsLine2() - range.getVcsLine1());
    }

    private void markLinesUnchanged(int startLine, int endLine) {
        if (myDocument.getTextLength() == 0) {
            return; // empty document has no lines
        }
        if (startLine == endLine) {
            return;
        }
        ((DocumentEx) myDocument).clearLineModificationFlags(startLine, endLine);
    }
}
