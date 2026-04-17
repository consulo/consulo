package consulo.execution.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.icon.ExecutionCoverageIconGroup;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.execution.executor.Executor;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;

@ExtensionImpl(id = "coverage", order = "after debug")
public class CoverageExecutor extends Executor {
    public static final String EXECUTOR_ID = "Coverage";

    @Override
    public LocalizeValue getStartActionText() {
        return ExecutionCoverageLocalize.runWithCoverage();
    }

    @Override
    public LocalizeValue getStartActiveText(String configurationName) {
        return ExecutionCoverageLocalize.runWithCoverageMnemonic(configurationName);
    }

    @Override
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public Image getToolWindowIcon() {
        return ExecutionCoverageIconGroup.toolwindowcoverage();
    }

    @Override
    public Image getIcon() {
        return ExecutionCoverageIconGroup.runwithcoverage();
    }

    @Override
    public Image getToolWindowIconIfRunning() {
        return ExecutionCoverageIconGroup.toolwindowcoverageactive();
    }

    @Override
    public LocalizeValue getDescription() {
        return ExecutionCoverageLocalize.runSelectedConfigurationWithCoverageEnabled();
    }

    @Override
    public LocalizeValue getActionName() {
        return ExecutionCoverageLocalize.actionNameCover();
    }

    @Override
    public String getId() {
        return EXECUTOR_ID;
    }

    @Override
    public String getContextActionId() {
        return "RunCoverage";
    }
}
