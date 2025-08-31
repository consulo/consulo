/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.document.internal.DocumentEx;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.SelectInEditorManager;
import consulo.document.DocumentWindow;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author MYakovlev
 * @since 2002-07-01
 */
@Singleton
@ServiceImpl
public class SelectInEditorManagerImpl implements SelectInEditorManager, Disposable, FocusListener, CaretListener {
  private final Project myProject;
  private RangeHighlighter mySegmentHighlighter;
  private Editor myEditor;

  @Inject
  public SelectInEditorManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
    releaseAll();
  }

  @Override
  public void selectInEditor(VirtualFile file, int startOffset, int endOffset, final boolean toSelectLine, final boolean toUseNormalSelection) {
    releaseAll();
    final TextRange textRange;
    if (file instanceof VirtualFileWindow) {
      DocumentWindow documentWindow = ((VirtualFileWindow)file).getDocumentWindow();
      textRange = documentWindow.injectedToHost(new TextRange(startOffset, endOffset));
      file = ((VirtualFileWindow)file).getDelegate();
    }
    else {
      textRange = new ProperTextRange(startOffset, endOffset);
    }
    openEditor(file, endOffset);
    final Editor editor = openEditor(file, textRange.getStartOffset());

    SwingUtilities.invokeLater(new Runnable() { // later to let focus listener chance to handle events
      @Override
      public void run() {
        if (editor != null && !editor.isDisposed()) {
          doSelect(toUseNormalSelection, editor, toSelectLine, textRange);
        }
      }
    });
  }

  private void doSelect(boolean toUseNormalSelection, @Nonnull Editor editor, boolean toSelectLine, TextRange textRange) {
    int startOffset = textRange.getStartOffset();
    int endOffset = textRange.getEndOffset();
    if (toUseNormalSelection) {
      DocumentEx doc = (DocumentEx)editor.getDocument();
      if (toSelectLine) {
        int lineNumber = doc.getLineNumber(startOffset);
        if (lineNumber >= 0 && lineNumber < doc.getLineCount()) {
          editor.getSelectionModel().setSelection(doc.getLineStartOffset(lineNumber), doc.getLineEndOffset(lineNumber) + doc.getLineSeparatorLength(lineNumber));
        }
      }
      else {
        editor.getSelectionModel().setSelection(startOffset, endOffset);
      }
      return;
    }

    TextAttributes selectionAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);

    releaseAll();

    if (toSelectLine) {
      DocumentEx doc = (DocumentEx)editor.getDocument();
      int lineNumber = doc.getLineNumber(startOffset);
      if (lineNumber >= 0 && lineNumber < doc.getLineCount()) {
        mySegmentHighlighter = editor.getMarkupModel()
                .addRangeHighlighter(doc.getLineStartOffset(lineNumber), doc.getLineEndOffset(lineNumber) + doc.getLineSeparatorLength(lineNumber), HighlighterLayer.LAST + 1, selectionAttributes,
                                     HighlighterTargetArea.EXACT_RANGE);
      }
    }
    else {
      mySegmentHighlighter = editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, HighlighterLayer.LAST + 1, selectionAttributes, HighlighterTargetArea.EXACT_RANGE);
    }
    myEditor = editor;
    myEditor.getContentComponent().addFocusListener(this);
    myEditor.getCaretModel().addCaretListener(this);
  }

  @Override
  public void focusGained(FocusEvent e) {
    releaseAll();
  }

  @Override
  public void focusLost(FocusEvent e) {
  }

  @Override
  public void caretPositionChanged(CaretEvent e) {
    releaseAll();
  }

  @Override
  public void caretAdded(CaretEvent e) {
  }

  @Override
  public void caretRemoved(CaretEvent e) {
  }

  private void releaseAll() {
    if (mySegmentHighlighter != null && myEditor != null) {
      mySegmentHighlighter.dispose();
      myEditor.getContentComponent().removeFocusListener(this);
      myEditor.getCaretModel().removeCaretListener(this);
      mySegmentHighlighter = null;
      myEditor = null;
    }
  }

  @Nullable
  private Editor openEditor(VirtualFile file, int textOffset) {
    if (file == null || !file.isValid()) {
      return null;
    }
    OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(myProject, file, textOffset);
    return FileEditorManager.getInstance(myProject).openTextEditor(descriptor, false);
  }
}
