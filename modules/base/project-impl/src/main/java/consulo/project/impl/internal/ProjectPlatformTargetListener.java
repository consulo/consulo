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
package consulo.project.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetListener;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.CoroutineScope;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Objects;

/**
 * The settings of a project are not saved while its machine is not connected - they are saved once it is connected again.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicImpl(ComponentScope.APPLICATION)
public class ProjectPlatformTargetListener implements PlatformTargetListener {
    private final Application myApplication;
    private final Provider<ProjectManager> myProjectManager;

    @Inject
    public ProjectPlatformTargetListener(Application application, Provider<ProjectManager> projectManager) {
        myApplication = application;
        myProjectManager = projectManager;
    }

    @Override
    public void targetConnected(PlatformTarget target) {
        UIAccess uiAccess = myApplication.getLastUIAccess();
        for (Project project : myProjectManager.get().getOpenProjects()) {
            if (!project.isDisposed() && Objects.equals(project.getPlatformTargetId(), target.getId())) {
                project.saveAsync(uiAccess).runAsync(CoroutineScope.of(project.coroutineContext()), null);
            }
        }
    }
}
