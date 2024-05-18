// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.AllIcons;
import consulo.execution.ExecutionBundle;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
@ActionImpl(id = "RunDashboard.Run")
public final class RunAction extends ExecutorAction {
  @Override
  protected Executor getExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  @Nullable
  @Override
  protected Image getTemplateIcon() {
    return PlatformIconGroup.actionsExecute();
  }

  @Override
  protected void update(@Nonnull AnActionEvent e, boolean running) {
    Presentation presentation = e.getPresentation();
    if (running) {
      presentation.setText(ExecutionBundle.message("run.dashboard.rerun.action.name"));
      presentation.setDescription(ExecutionBundle.message("run.dashboard.rerun.action.description"));
      presentation.setIcon( AllIcons.Actions.Restart);
    }
    else {
      presentation.setText(ExecutionBundle.message("run.dashboard.run.action.name"));
      presentation.setDescription(ExecutionBundle.message("run.dashboard.run.action.description"));
      presentation.setIcon(AllIcons.Actions.Execute);
    }
  }
}
