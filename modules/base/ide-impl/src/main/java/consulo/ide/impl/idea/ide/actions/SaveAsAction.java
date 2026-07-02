package consulo.ide.impl.idea.ide.actions;

import consulo.language.editor.refactoring.copy.CopyHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;

public class SaveAsAction extends LegacyDumbAwareAction {
    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && e.hasData(VirtualFile.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        VirtualFile virtualFile = e.getRequiredData(VirtualFile.KEY);
        @SuppressWarnings({"ConstantConditions"}) PsiElement element = PsiManager.getInstance(project).findFile(virtualFile);
        if (element == null) {
            return;
        }
        CopyHandler.doCopy(new PsiElement[]{element.getContainingFile()}, element.getContainingFile().getContainingDirectory());
    }
}
