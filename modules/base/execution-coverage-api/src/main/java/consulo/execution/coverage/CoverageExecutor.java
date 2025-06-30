package consulo.execution.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.icon.ExecutionCoverageIconGroup;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.execution.executor.Executor;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "coverage", order = "after debug")
public class CoverageExecutor extends Executor {
    public static final String EXECUTOR_ID = "Coverage";

    @Nonnull
    @Override
    public LocalizeValue getStartActionText() {
        return ExecutionCoverageLocalize.runWithCoverage();
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActiveText(@Nonnull String configurationName) {
        return ExecutionCoverageLocalize.runWithCoverageMnemonic(configurationName);
    }

    @Nonnull
    @Override
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public Image getToolWindowIcon() {
        return ExecutionCoverageIconGroup.toolwindowcoverage();
    }

    @Override
    @Nonnull
    public Image getIcon() {
        return ExecutionCoverageIconGroup.runwithcoverage();
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return ExecutionCoverageLocalize.runSelectedConfigurationWithCoverageEnabled();
    }

    @Override
    @Nonnull
    public LocalizeValue getActionName() {
        return ExecutionCoverageLocalize.actionNameCover();
    }

    @Override
    @Nonnull
    public String getId() {
        return EXECUTOR_ID;
    }

    @Nonnull
    @Override
    public String getContextActionId() {
        return "RunCoverage";
    }

    @Override
    public String getHelpId() {
        return null;
    }
}
