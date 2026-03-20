/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.project;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Provides project management.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectManager {

    /**
     * Gets <code>ProjectManager</code> instance.
     *
     * @return <code>ProjectManager</code> instance
     */
    
    public static ProjectManager getInstance() {
        return Application.get().getInstance(ProjectManager.class);
    }

    
    AsyncResult<Project> openProjectAsync(VirtualFile file, UIAccess uiAccess, ProjectOpenContext context);

    
    default AsyncResult<Project> openProjectAsync(VirtualFile file, UIAccess uiAccess) {
        return openProjectAsync(file, uiAccess, new ProjectOpenContext());
    }

    
    AsyncResult<Project> openProjectAsync(Project project, UIAccess uiAccess, ProjectOpenContext context);

    default AsyncResult<Project> openProjectAsync(Project project, UIAccess uiAccess) {
        return openProjectAsync(project, uiAccess, new ProjectOpenContext());
    }

    
    default AsyncResult<Void> closeAndDisposeAsync(Project project, UIAccess uiAccess) {
        return closeAndDisposeAsync(project, uiAccess, true, true, true);
    }

    boolean isProjectOpened(Project project);

    
    AsyncResult<Void> closeAndDisposeAsync(Project project, UIAccess uiAccess, boolean checkCanClose, boolean save, boolean dispose);

    /**
     * Adds listener to the specified project.
     *
     * @param project  project to add listener to
     * @param listener listener to add
     */
    void addProjectManagerListener(Project project, ProjectManagerListener listener);

    /**
     * Removes listener from the specified project.
     *
     * @param project  project to remove listener from
     * @param listener listener to remove
     */
    void removeProjectManagerListener(Project project, ProjectManagerListener listener);

    /**
     * Returns the list of currently opened projects.
     *
     * @return the array of currently opened projects.
     */
    
    Project[] getOpenProjects();

    /**
     * Returns the project which is used as a template for new projects. The template project
     * is always available, even when no other project is open.
     * NB: default project can be lazy loaded
     *
     * @return the template project instance.
     */
    
    default Project getDefaultProject() {
        return DefaultProjectFactory.getInstance().getDefaultProject();
    }

    /**
     * Closes the specified project.
     *
     * @param project the project to close.
     * @return true if the project was closed successfully, false if the closing was disallowed by the close listeners.
     */
    @RequiredUIAccess
    boolean closeProject(Project project);

    /**
     * Asynchronously reloads the specified project.
     *
     * @param project the project to reload.
     */
    void reloadProject(Project project, UIAccess uiAccess);

    /**
     * Create new project in given location.
     *
     * @param name project name
     * @param path project location
     * @return newly crated project
     */
    @Nullable Project createProject(String name, String path);

    // region deprecated code

    /**
     * Adds global listener to all projects
     *
     * @param listener listener to add
     */
    @Deprecated
    @DeprecationInfo("Use ProjectManager#TOPIC")
    void addProjectManagerListener(ProjectManagerListener listener);

    @Deprecated
    @DeprecationInfo("Use ProjectManager#TOPIC")
    void addProjectManagerListener(ProjectManagerListener listener, Disposable parentDisposable);

    /**
     * Removes global listener from all projects.
     *
     * @param listener listener to remove
     */
    @Deprecated
    @DeprecationInfo("Use ProjectManager#TOPIC")
    void removeProjectManagerListener(ProjectManagerListener listener);

    //endregion
}
