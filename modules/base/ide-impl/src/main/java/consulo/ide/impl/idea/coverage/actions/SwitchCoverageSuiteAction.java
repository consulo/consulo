package consulo.ide.impl.idea.coverage.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
public class SwitchCoverageSuiteAction extends AnAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getDataContext().getData(Project.KEY);
        new CoverageSuiteChooserDialog(project).show();
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        Project project = e.getDataContext().getData(Project.KEY);
        e.getPresentation().setEnabled(project != null);
    }
}
