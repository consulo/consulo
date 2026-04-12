// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author konstantin.aleev
 */
@ActionImpl(id = "RunDashboard.Run")
public final class RunAction extends ExecutorAction {
  @Override
  protected Executor getExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  @Override
  protected @Nullable Image getTemplateIcon() {
    return PlatformIconGroup.actionsExecute();
  }

  @Override
  protected void update(AnActionEvent e, boolean running) {
    Presentation presentation = e.getPresentation();
    if (running) {
      presentation.setText(ExecutionLocalize.runDashboardRerunActionName());
      presentation.setDescription(ExecutionLocalize.runDashboardRerunActionDescription());
      presentation.setIcon(PlatformIconGroup.actionsRestart());
    }
    else {
      presentation.setText(ExecutionLocalize.runDashboardRunActionName());
      presentation.setDescription(ExecutionLocalize.runDashboardRunActionDescription());
      presentation.setIcon(PlatformIconGroup.actionsExecute());
    }
  }
}
