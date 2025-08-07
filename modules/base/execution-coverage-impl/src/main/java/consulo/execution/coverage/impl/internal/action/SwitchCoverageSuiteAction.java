package consulo.execution.coverage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
@ActionImpl(id = "SwitchCoverage")
public class SwitchCoverageSuiteAction extends AnAction {
    public SwitchCoverageSuiteAction() {
        super(ExecutionCoverageLocalize.actionSwitchCoverageText(), ExecutionCoverageLocalize.actionSwitchCoverageDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        new CoverageSuiteChooserDialog(project).show();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }
}
