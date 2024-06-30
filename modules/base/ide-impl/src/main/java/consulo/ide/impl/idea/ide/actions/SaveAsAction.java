package consulo.ide.impl.idea.ide.actions;

import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.copy.CopyHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;

public class SaveAsAction extends DumbAwareAction {
  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    final VirtualFile virtualFile = dataContext.getData(VirtualFile.KEY);
    e.getPresentation().setEnabled(project!=null && virtualFile!=null);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    final VirtualFile virtualFile = dataContext.getData(VirtualFile.KEY);
    @SuppressWarnings({"ConstantConditions"}) final PsiElement element = PsiManager.getInstance(project).findFile(virtualFile);
    if (element == null) {
      return;
    }
    CopyHandler.doCopy(new PsiElement[] {element.getContainingFile()}, element.getContainingFile().getContainingDirectory());
  }
}
