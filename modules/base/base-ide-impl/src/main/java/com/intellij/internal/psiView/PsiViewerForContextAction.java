package com.intellij.internal.psiView;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

/**
 * Created by IntelliJ IDEA.
 * User: Nadya.Zabrodina
 * Date: 7/4/11
 * Time: 4:16 PM
 */
public class PsiViewerForContextAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(AnActionEvent e) {

    Editor editor = e.getDataContext().getData(PlatformDataKeys.EDITOR);
    PsiFile currentFile = e.getDataContext().getData(LangDataKeys.PSI_FILE);
    new PsiViewerDialog(currentFile.getProject(), false, currentFile, editor).show();
  }

  @Override
  public void update(AnActionEvent e) {
    if (!Application.get().isInternal()) {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
      return;
    }
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    PsiFile currentFile = e.getDataContext().getData(LangDataKeys.PSI_FILE);
    e.getPresentation().setEnabled(project != null && currentFile != null);
  }
}
