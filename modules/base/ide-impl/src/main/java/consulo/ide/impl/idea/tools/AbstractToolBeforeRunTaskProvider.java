/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tools;

import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * @author traff
 */
public abstract class AbstractToolBeforeRunTaskProvider<T extends AbstractToolBeforeRunTask> extends BeforeRunTaskProvider<T> {
  protected static final Logger LOG = Logger.getInstance(AbstractToolBeforeRunTaskProvider.class);

  @Override
  public Image getIcon() {
    return PlatformIconGroup.generalExternaltools();
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, T task) {
    ToolSelectDialog dialog = new ToolSelectDialog(runConfiguration.getProject(), task.getToolActionId(), createToolsPanel());

    AsyncResult<Void> result = AsyncResult.undefined();

    AsyncResult<Void> showAsync = dialog.showAsync();
    showAsync.doWhenDone(() -> {
      boolean isModified = dialog.isModified();
      Tool selectedTool = dialog.getSelectedTool();
      LOG.assertTrue(selectedTool != null);
      String selectedToolId = selectedTool.getActionId();
      String oldToolId = task.getToolActionId();
      if (oldToolId != null && oldToolId.equals(selectedToolId)) {
        if (isModified) {
          result.setDone();
        }
        else {
          result.setRejected();
        }
        return;
      }
      task.setToolActionId(selectedToolId);
      result.setDone();
    });
    showAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  protected abstract BaseToolsPanel createToolsPanel();

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, T task) {
    return task.isExecutable();
  }

  @Nonnull
  @Override
  public String getDescription(T task) {
    String actionId = task.getToolActionId();
    if (actionId == null) {
      LOG.error("Null id");
      return ToolsBundle.message("tools.unknown.external.tool");
    }
    Tool tool = task.findCorrespondingTool();
    if (tool == null) {
      return ToolsBundle.message("tools.unknown.external.tool");
    }
    String groupName = tool.getGroup();
    return ToolsBundle.message("tools.before.run.description", StringUtil.isEmpty(groupName) ? tool.getName() : groupName + "/" + tool.getName());
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, T task) {
    if (!task.isExecutable()) {
      return AsyncResult.rejected();
    }
    return task.execute(uiAccess, context, env.getExecutionId());
  }
}
