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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ProjectCloseHandler;
import consulo.project.event.ProjectManagerListener;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Inject;

@ExtensionImpl
public class LegacyProjectCloseHandler implements ProjectCloseHandler {
    private final Application myApplication;
    private final Project myProject;

    @Inject
    public LegacyProjectCloseHandler(Application application, Project project) {
        myApplication = application;
        myProject = project;
    }

    @Override
    public Coroutine<?, ?> beforeProjectClose() {
        return Coroutine.first(CodeExecution.<Object, Object>apply(input -> {
            myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectClosing(myProject);
            return input;
        }));
    }

    @Override
    public Coroutine<?, ?> projectClosed() {
        return Coroutine.first(CodeExecution.<Object, Object>apply(input -> {
            myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectClosed(myProject, myProject.getUIAccess());
            return input;
        }));
    }
}
