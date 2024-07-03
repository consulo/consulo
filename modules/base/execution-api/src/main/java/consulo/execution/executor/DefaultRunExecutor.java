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

package consulo.execution.executor;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@ExtensionImpl(id = "run", order = "first")
public class DefaultRunExecutor extends Executor {
  public static final String EXECUTOR_ID = ToolWindowId.RUN;

  @Override
  @Nonnull
  public String getStartActionText() {
    return ExecutionLocalize.defaultRunnerStartActionText().get();
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.RUN;
  }

  @Override
  public Image getToolWindowIcon() {
    return PlatformIconGroup.toolwindowsToolwindowrun();
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return PlatformIconGroup.actionsExecute();
  }

  @Override
  public String getDescription() {
    return ExecutionLocalize.standardRunnerDescription().get();
  }

  @Override
  @Nonnull
  public String getActionName() {
    return ExecutionLocalize.toolWindowNameRun().get();
  }

  @Override
  @Nonnull
  public String getId() {
    return EXECUTOR_ID;
  }

  @Override
  public String getContextActionId() {
    return "RunClass";
  }

  @Override
  public String getHelpId() {
    return "ideaInterface.run";
  }

  public static Executor getRunExecutorInstance() {
    return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
  }
}
