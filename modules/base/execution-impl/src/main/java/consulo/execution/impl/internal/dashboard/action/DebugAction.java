// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.ExecutionBundle;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

/**
 * @author konstantin.aleev
 */
@ActionImpl(id = "RunDashboard.Debug")
public final class DebugAction extends ExecutorAction {
  @Override
  protected Executor getExecutor() {
    return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
  }

  @Nullable
  @Override
  protected Image getTemplateIcon() {
    return PlatformIconGroup.actionsStartdebugger();
  }

  @Override
  protected void update(@Nonnull AnActionEvent e, boolean running) {
    Presentation presentation = e.getPresentation();
    if (running) {
      presentation.setText(ExecutionBundle.message("run.dashboard.restart.debugger.action.name"));
      presentation.setDescription(ExecutionBundle.message("run.dashboard.restart.debugger.action.description"));
      presentation.setIcon(PlatformIconGroup.actionsRestartdebugger());
    }
    else {
      presentation.setText(ExecutionBundle.message("run.dashboard.debug.action.name"));
      presentation.setDescription(ExecutionBundle.message("run.dashboard.debug.action.description"));
      presentation.setIcon(PlatformIconGroup.actionsStartdebugger());
    }
  }
}
