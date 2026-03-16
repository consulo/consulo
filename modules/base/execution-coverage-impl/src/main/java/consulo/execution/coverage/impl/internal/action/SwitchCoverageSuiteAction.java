package consulo.execution.coverage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

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
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        new CoverageSuiteChooserDialog(project).show();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }
}
