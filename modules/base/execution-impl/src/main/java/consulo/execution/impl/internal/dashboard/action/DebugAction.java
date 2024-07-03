// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
      presentation.setTextValue(ExecutionLocalize.runDashboardRestartDebuggerActionName());
      presentation.setDescriptionValue(ExecutionLocalize.runDashboardRestartDebuggerActionDescription());
      presentation.setIcon(PlatformIconGroup.actionsRestartdebugger());
    }
    else {
      presentation.setTextValue(ExecutionLocalize.runDashboardDebugActionName());
      presentation.setDescriptionValue(ExecutionLocalize.runDashboardDebugActionDescription());
      presentation.setIcon(PlatformIconGroup.actionsStartdebugger());
    }
  }
}
