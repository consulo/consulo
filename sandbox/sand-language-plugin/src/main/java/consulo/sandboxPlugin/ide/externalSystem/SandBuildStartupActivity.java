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
package consulo.sandboxPlugin.ide.externalSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

@ExtensionImpl
public class SandBuildStartupActivity implements PostStartupActivity, DumbAware {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        SandBuildProjectAware projectAware = new SandBuildProjectAware(basePath);
        ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
        projectTracker.register(projectAware, project);
        projectTracker.activate(projectAware.getProjectId());
    }
}
