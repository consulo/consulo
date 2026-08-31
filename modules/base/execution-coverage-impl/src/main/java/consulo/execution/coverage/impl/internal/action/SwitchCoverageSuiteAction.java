package consulo.execution.coverage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyAnAction;

/**
 * @author ven
 */
@ActionImpl(id = "SwitchCoverage")
public class SwitchCoverageSuiteAction extends LegacyAnAction {
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
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }
}
