/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.UIBundle;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@ExtensionImpl(id = "debug", order = "after run")
public class DefaultDebugExecutor extends Executor {
  public static final String EXECUTOR_ID = ToolWindowId.DEBUG;

  @Override
  public String getToolWindowId() {
    return ToolWindowId.DEBUG;
  }

  @Override
  public Image getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowDebugger;
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return AllIcons.Actions.StartDebugger;
  }

  @Override
  @Nonnull
  public String getActionName() {
    return UIBundle.message("tool.window.name.debug");
  }

  @Override
  @Nonnull
  public String getId() {
    return EXECUTOR_ID;
  }

  @Override
  public String getContextActionId() {
    return "DebugClass";
  }

  @Override
  @Nonnull
  public String getStartActionText() {
    return XDebuggerBundle.message("debugger.runner.start.action.text");
  }

  @Override
  public String getDescription() {
    return XDebuggerBundle.message("string.debugger.runner.description");
  }

  @Override
  public String getHelpId() {
    return "debugging.DebugWindow";
  }

  public static Executor getDebugExecutorInstance() {
    return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
  }
}
