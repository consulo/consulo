/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.view;

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Leaf node representing a single run configuration tied to an external system task.
 *
 * @author Vladislav.Soroka
 */
public class RunConfigurationNode extends ExternalSystemNode<Object> {
    private final RunnerAndConfigurationSettings mySettings;

    public RunConfigurationNode(ExternalProjectsView externalProjectsView,
                                RunConfigurationsNode parent,
                                RunnerAndConfigurationSettings settings) {
        super(externalProjectsView, parent, null);
        mySettings = settings;
    }

    @Override
    public String getName() {
        return mySettings.getName();
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(mySettings.getConfiguration().getIcon());

        String shortcutHint = StringUtil.nullize(getShortcutsManager().getDescription(mySettings));
        setNameAndTooltip(presentation, getName(), null, shortcutHint);
    }

    @Override
    public boolean isAlwaysLeaf() {
        return true;
    }

    @Override
    @Nullable
    protected String getMenuId() {
        return "ExternalSystemView.RunConfigurationMenu";
    }

    @Override
    @Nullable
    protected String getActionId() {
        return "ExternalSystem.RunConfiguration";
    }

   
    public RunnerAndConfigurationSettings getSettings() {
        return mySettings;
    }
}
