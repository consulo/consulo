package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import consulo.ui.RequiredUIAccess;
import consulo.application.AccessRule;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author yole
 */
public class SaveDocumentAction extends AnAction {
  private final FileDocumentManager myFileDocumentManager;

  @Inject
  public SaveDocumentAction(FileDocumentManager fileDocumentManager) {
    myFileDocumentManager = fileDocumentManager;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Document doc = getDocument(e);
    if(doc == null) {
      return;
    }
    AccessRule.writeAsync(() -> myFileDocumentManager.saveDocument(doc));
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(getDocument(e) != null);
  }

  private static Document getDocument(AnActionEvent e) {
    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    return editor != null ? editor.getDocument() : null;
  }
}
