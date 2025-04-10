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
package consulo.externalSystem.impl.internal.service;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.service.project.ProjectRenameAware;
import consulo.externalSystem.impl.internal.service.project.autoimport.ExternalSystemAutoImporter;
import consulo.externalSystem.impl.internal.service.ui.ExternalToolWindowManager;
import consulo.externalSystem.impl.internal.service.vcs.ExternalSystemVcsRegistrar;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.startup.StartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 2013-05-02
 */
@ExtensionImpl
public class ExternalSystemStartupActivity implements BackgroundStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        Application.get().invokeLater(
            () -> {
                for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
                    if (manager instanceof StartupActivity startupActivity) {
                        startupActivity.runActivity(project, uiAccess);
                    }
                }
                if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) != Boolean.TRUE) {
                    for (ExternalSystemManager manager : ExternalSystemManager.EP_NAME.getExtensionList()) {
                        ExternalSystemUtil.refreshProjects(project, manager.getSystemId(), false);
                    }
                }
                ExternalSystemAutoImporter.letTheMagicBegin(project);
                ExternalToolWindowManager.handle(project);
                ExternalSystemVcsRegistrar.handle(project);
                ProjectRenameAware.beAware(project);
            },
            project.getDisposed()
        );
    }
}
