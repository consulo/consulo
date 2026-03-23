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

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.externalSystem.util.Order;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.tree.PresentationData;

import java.util.ArrayList;
import java.util.List;

/**
 * Container node for run configurations associated with a module's external project path.
 *
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_RUN_CONFIGURATIONS_DATA_NODE_ORDER)
public class RunConfigurationsNode extends ExternalSystemNode<Object> {
    private final ModuleNode myModuleNode;

    public RunConfigurationsNode(ExternalProjectsView externalProjectsView,
                                 ModuleNode moduleNode) {
        super(externalProjectsView, moduleNode, null);
        myModuleNode = moduleNode;
    }

    @Override
    public String getName() {
        return "Run Configurations";
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(PlatformIconGroup.nodesConfigfolder());
    }

    @Override
    public boolean isVisible() {
        return hasChildren();
    }

    @Override
   
    protected List<ExternalSystemNode<?>> doBuildChildren() {
        return buildRunConfigurationNodes();
    }

    public void updateRunConfigurations() {
        cleanUpCache();
    }

   
    private List<ExternalSystemNode<?>> buildRunConfigurationNodes() {
        List<ExternalSystemNode<?>> result = new ArrayList<>();
        ModuleData moduleData = myModuleNode.getData();
        if (moduleData == null) return result;

        String projectPath = moduleData.getLinkedExternalProjectPath();
        RunManager runManager = RunManager.getInstance(getExternalProjectsView().getProject());

        for (RunnerAndConfigurationSettings settings : runManager.getAllSettings()) {
            if (settings.getConfiguration() instanceof ExternalSystemRunConfiguration runConfig) {
                String configPath = runConfig.getSettings().getExternalProjectPath();
                if (projectPath.equals(configPath) &&
                    getExternalProjectsView().getSystemId().getId().equals(runConfig.getSettings().getExternalSystemIdString())) {
                    result.add(new RunConfigurationNode(getExternalProjectsView(), this, settings));
                }
            }
        }
        return result;
    }
}
