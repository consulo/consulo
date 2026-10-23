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
package consulo.externalTool.impl.internal;

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunFragment;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ListBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author traff
 */
public abstract class AbstractToolBeforeRunTaskProvider<T extends AbstractToolBeforeRunTask<?>> implements BeforeRunFragment<T> {
    protected static final Logger LOG = Logger.getInstance(AbstractToolBeforeRunTaskProvider.class);

    @Override
    public Image getIcon(RunConfiguration configuration) {
        return PlatformIconGroup.generalExternaltools();
    }

    protected abstract List<? extends Tool> getTools();

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(RunConfiguration configuration, Disposable parent) {
        List<Tool> tools = new ArrayList<>(getTools());
        ListBox<Tool> list = ListBox.create(tools);
        list.setRenderer((presentation, item) -> {
            Tool tool = item.getValue();
            if (tool != null) {
                String group = tool.getGroup();
                presentation.append(StringUtil.isEmpty(group) ? tool.getName() : group + "/" + tool.getName());
            }
        });

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(list);
            }

            @RequiredUIAccess
            @Override
            public void reset(T task) {
                for (int i = 0; i < tools.size(); i++) {
                    if (tools.get(i).getActionId().equals(task.getToolActionId())) {
                        list.setValueByIndex(i);
                        return;
                    }
                }
            }

            @RequiredUIAccess
            @Override
            public void apply(T task) {
                Tool tool = list.getValue();
                if (tool != null) {
                    task.setToolActionId(tool.getActionId());
                }
            }
        };
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, T task) {
        return task.isExecutable();
    }

    @Override
    public LocalizeValue getDescription(T task) {
        String actionId = task.getToolActionId();
        if (actionId == null) {
            return ExternalToolLocalize.toolsUnknownExternalTool();
        }
        Tool tool = task.findCorrespondingTool();
        if (tool == null) {
            return ExternalToolLocalize.toolsUnknownExternalTool();
        }
        String groupName = tool.getGroup();
        return ExternalToolLocalize.toolsBeforeRunDescription(StringUtil.isEmpty(groupName) ? tool.getName() : groupName + "/" + tool.getName());
    }

    @Override
    public CompletableFuture<?> executeAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, T task) {
        if (!task.isExecutable()) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        return task.execute(uiAccess, context, env.getExecutionId());
    }
}
