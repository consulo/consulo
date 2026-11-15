/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.impl.internal.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;
import org.jdom.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Run configurations follow plugins loaded or unloaded at runtime: the state is read again, configurations of an
 * unloaded type become unknown ones and come back with the plugin.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@TopicImpl(ComponentScope.APPLICATION)
public class RunManagerDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;
    private final Map<Project, Pair<Element, Element>> myStates = new HashMap<>();

    @Inject
    public RunManagerDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public void beforePluginsChange(DynamicPluginChange change) {
        myApplication.runWriteAction(() -> {
            myStates.clear();
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (project.isDisposed()) {
                    continue;
                }

                RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
                ProjectRunConfigurationManager sharedManager = project.getInstance(ProjectRunConfigurationManager.class);
                myStates.put(project, Pair.create(runManager.getState(), sharedManager.getState()));
            }
        });
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        myApplication.runWriteAction(() -> {
            for (Map.Entry<Project, Pair<Element, Element>> entry : myStates.entrySet()) {
                Project project = entry.getKey();
                if (project.isDisposed()) {
                    continue;
                }

                Element workspaceState = entry.getValue().getFirst();
                if (workspaceState != null) {
                    RunManagerImpl.getInstanceImpl(project).loadState(workspaceState);
                }
                project.getInstance(ProjectRunConfigurationManager.class).loadState(entry.getValue().getSecond());
            }
            myStates.clear();
        });
    }
}
