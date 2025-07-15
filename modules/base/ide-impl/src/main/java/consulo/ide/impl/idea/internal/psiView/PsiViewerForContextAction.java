package consulo.ide.impl.idea.internal.psiView;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Nadya.Zabrodina
 * @since 2011-07-04
 */
public class PsiViewerForContextAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Editor editor = e.getRequiredData(Editor.KEY);
    PsiFile currentFile = e.getRequiredData(PsiFile.KEY);
    new PsiViewerDialog(currentFile.getProject(), false, currentFile, editor).show();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (!Application.get().isInternal()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    e.getPresentation().setEnabled(e.hasData(Project.KEY) && e.hasData(PsiFile.KEY));
  }
}
