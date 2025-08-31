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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.impl.DocumentImpl;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

import static consulo.diff.impl.internal.util.DiffImplUtil.getLineCount;

/**
 * @author irengrig
 *         author: lesya
 */
@SuppressWarnings({"MethodMayBeStatic", "FieldAccessedSynchronizedAndUnsynchronized"})
public class LineStatusTracker extends LineStatusTrackerBase {
  public enum Mode {DEFAULT, SMART, SILENT}

  private static final Key<JPanel> PANEL_KEY = new Key<>("LineStatusTracker.CanNotCalculateDiffPanel");

  @Nonnull
  private final VirtualFile myVirtualFile;

  @Nonnull
  private final FileEditorManager myFileEditorManager;
  @Nonnull
  private final VcsDirtyScopeManager myVcsDirtyScopeManager;

  @Nonnull
  private Mode myMode;

  private LineStatusTracker(@Nonnull Project project,
                            @Nonnull Document document,
                            @Nonnull VirtualFile virtualFile,
                            @Nonnull Mode mode) {
    super(project, document);
    myVirtualFile = virtualFile;
    myMode = mode;

    myFileEditorManager = FileEditorManager.getInstance(project);
    myVcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
  }

  public static LineStatusTracker createOn(@Nonnull VirtualFile virtualFile,
                                           @Nonnull Document document,
                                           @Nonnull Project project,
                                           @Nonnull Mode mode) {
    return new LineStatusTracker(project, document, virtualFile, mode);
  }

  @Nonnull
  @Override
  public Project getProject() {
    //noinspection ConstantConditions
    return super.getProject();
  }

  @Nonnull
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Nonnull
  @RequiredUIAccess
  public Mode getMode() {
    return myMode;
  }

  @RequiredUIAccess
  public boolean isSilentMode() {
    return myMode == Mode.SILENT;
  }

  @RequiredUIAccess
  public void setMode(@Nonnull Mode mode) {
    if (myMode == mode) return;
    myMode = mode;

    reinstallRanges();
  }

  @Override
  @RequiredUIAccess
  protected boolean isDetectWhitespaceChangedLines() {
    return myMode == Mode.SMART;
  }

  @Override
  @RequiredUIAccess
  protected void installNotification(@Nonnull String text) {
    FileEditor[] editors = myFileEditorManager.getAllEditors(myVirtualFile);
    for (FileEditor editor : editors) {
      JPanel panel = editor.getUserData(PANEL_KEY);
      if (panel == null) {
        JPanel newPanel = new EditorNotificationPanel().text(text);
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
      JPanel panel = editor.getUserData(PANEL_KEY);
      if (panel != null) {
        myFileEditorManager.removeTopComponent(editor, panel);
        editor.putUserData(PANEL_KEY, null);
      }
    }
  }

  @Override
  @RequiredUIAccess
  protected void createHighlighter(@Nonnull Range range) {
    UIAccess.assertIsUIThread();

    if (range.getHighlighter() != null) {
      LOG.error("Multiple highlighters registered for the same Range");
      return;
    }

    if (myMode == Mode.SILENT) return;

    int first =
            range.getLine1() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine1());
    int second =
            range.getLine2() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine2());

    MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, true);

    RangeHighlighter highlighter = LineStatusMarkerRenderer.createRangeHighlighter(range, new TextRange(first, second), markupModel);
    highlighter.setLineMarkerRenderer(LineStatusMarkerRenderer.createRenderer(range, (editor) -> {
      return new LineStatusTrackerDrawing.MyLineStatusMarkerPopup(this, editor, range);
    }));

    range.setHighlighter(highlighter);
  }

  @Override
  @RequiredUIAccess
  protected void fireFileUnchanged() {
    // later to avoid saving inside document change event processing.
    FileDocumentManager.getInstance().saveDocument(myDocument);
    List<Range> ranges = getRanges();
    if (ranges == null || ranges.isEmpty()) {
      // file was modified, and now it's not -> dirty local change
      myVcsDirtyScopeManager.fileDirty(myVirtualFile);
    }
  }

  @Override
  protected void doRollbackRange(@Nonnull Range range) {
    super.doRollbackRange(range);
    markLinesUnchanged(range.getLine1(), range.getLine1() + range.getVcsLine2() - range.getVcsLine1());
  }

  private void markLinesUnchanged(int startLine, int endLine) {
    if (myDocument.getTextLength() == 0) return; // empty document has no lines
    if (startLine == endLine) return;
    ((DocumentImpl)myDocument).clearLineModificationFlags(startLine, endLine);
  }
}
