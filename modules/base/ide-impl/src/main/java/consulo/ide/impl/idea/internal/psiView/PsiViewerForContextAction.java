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
 * Created by IntelliJ IDEA.
 * User: Nadya.Zabrodina
 * Date: 7/4/11
 * Time: 4:16 PM
 */
public class PsiViewerForContextAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Editor editor = e.getDataContext().getData(Editor.KEY);
    PsiFile currentFile = e.getDataContext().getData(PsiFile.KEY);
    new PsiViewerDialog(currentFile.getProject(), false, currentFile, editor).show();
  }

  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent e) {
    if (!Application.get().isInternal()) {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
      return;
    }
    final Project project = e.getDataContext().getData(Project.KEY);
    PsiFile currentFile = e.getDataContext().getData(PsiFile.KEY);
    e.getPresentation().setEnabled(project != null && currentFile != null);
  }
}
