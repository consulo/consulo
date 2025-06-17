package consulo.execution.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.executor.Executor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "coverage", order = "after debug")
public class CoverageExecutor extends Executor {
    public static final String EXECUTOR_ID = "Coverage";

    @Nonnull
    @Override
    public LocalizeValue getStartActionText() {
        return LocalizeValue.localizeTODO("Run with Co_verage");
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActiveText(@Nonnull String configurationName) {
        return LocalizeValue.localizeTODO("Run " + configurationName + " with Co_verage");
    }

    @Nonnull
    @Override
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public Image getToolWindowIcon() {
        return PlatformIconGroup.generalRunwithcoverage();
    }

    @Override
    @Nonnull
    public Image getIcon() {
        return PlatformIconGroup.generalRunwithcoverage();
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Run selected configuration with coverage enabled");
    }

    @Override
    @Nonnull
    public LocalizeValue getActionName() {
        return LocalizeValue.localizeTODO("Cover");
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
