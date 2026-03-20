/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.project.internal;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface ProjectManagerEx extends ProjectManager {
    public static ProjectManagerEx getInstanceEx() {
        return (ProjectManagerEx) ProjectManager.getInstance();
    }

    /**
     * @param dirPath path to directory where .consulo directory is located
     */
    @Nullable Project newProject(String projectName, String dirPath, boolean useDefaultProjectSettings);

    // returns true on success
    @RequiredUIAccess
    boolean closeAndDispose(Project project);

    @Override
    default @Nullable Project createProject(String name, String path) {
        return newProject(name, path, true);
    }

    boolean canClose(Project project);

    @RequiredUIAccess
    boolean closeProject(Project project, boolean save, boolean dispose, boolean checkCanClose);

    
    Disposable registerCloseProjectVeto(Predicate<Project> projectVeto);

    
        //@ApiStatus.Internal
    String[] getAllExcludedUrls();
}
