package consulo.ide.impl.idea.internal.psiView;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

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
