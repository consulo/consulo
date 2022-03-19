// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.action.EditorActionHandler;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.project.Project;
import consulo.codeEditor.impl.EditorInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextEndAction extends TextComponentEditorAction {
  public TextEndAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandler {
    @Override
    public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      editor.getCaretModel().removeSecondaryCarets();
      int offset = editor.getDocument().getTextLength();
      if (editor instanceof EditorInternal) {
        editor.getCaretModel().moveToLogicalPosition(editor.offsetToLogicalPosition(offset).leanForward(true));
      }
      else {
        editor.getCaretModel().moveToOffset(offset);
      }
      editor.getSelectionModel().removeSelection();

      ScrollingModel scrollingModel = editor.getScrollingModel();
      scrollingModel.disableAnimation();
      scrollingModel.scrollToCaret(ScrollType.CENTER);
      scrollingModel.enableAnimation();

      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        IdeDocumentHistory instance = IdeDocumentHistory.getInstance(project);
        if (instance != null) {
          instance.includeCurrentCommandAsNavigation();
        }
      }
    }
  }
}
