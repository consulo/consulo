package consulo.execution.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.executor.Executor;
import consulo.application.AllIcons;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "coverage", order = "after debug")
public class CoverageExecutor extends Executor {

  public static final String EXECUTOR_ID = "Coverage";

  @Override
  @Nonnull
  public String getStartActionText() {
    return "Run with Co_verage";
  }

  @Nonnull
  @Override
  public String getStartActionText(boolean emptyName) {
    return "Run" + (emptyName ? "" :  " ''{0}''") + " with Co_verage";
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.RUN;
  }

  @Override
  public Image getToolWindowIcon() {
    return AllIcons.General.RunWithCoverage;
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return AllIcons.General.RunWithCoverage;
  }

  @Override
  public String getDescription() {
    return "Run selected configuration with coverage enabled";
  }

  @Override
  @Nonnull
  public String getActionName() {
    return "Cover";
  }

  @Override
  @Nonnull
  public String getId() {
    return EXECUTOR_ID;
  }

  @Override
  public String getContextActionId() {
    return "RunCoverage";
  }

  @Override
  public String getHelpId() {
    return null;
  }
}
