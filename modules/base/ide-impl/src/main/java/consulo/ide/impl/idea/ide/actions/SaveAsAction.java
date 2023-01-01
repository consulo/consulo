package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.editor.refactoring.copy.CopyHandler;

public class SaveAsAction extends DumbAwareAction {

  @Override
  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final VirtualFile virtualFile = dataContext.getData(PlatformDataKeys.VIRTUAL_FILE);
    e.getPresentation().setEnabled(project!=null && virtualFile!=null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final VirtualFile virtualFile = dataContext.getData(PlatformDataKeys.VIRTUAL_FILE);
    @SuppressWarnings({"ConstantConditions"}) final PsiElement element = PsiManager.getInstance(project).findFile(virtualFile);
    if(element==null) return;
    CopyHandler.doCopy(new PsiElement[] {element.getContainingFile()}, element.getContainingFile().getContainingDirectory());
  }
}
