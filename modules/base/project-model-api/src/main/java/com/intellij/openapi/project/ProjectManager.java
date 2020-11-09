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
package com.intellij.openapi.project;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Provides project management.
 */
public abstract class ProjectManager {
  public static final Topic<ProjectManagerListener> TOPIC = Topic.create("Project open and close events", ProjectManagerListener.class);

  /**
   * Gets <code>ProjectManager</code> instance.
   *
   * @return <code>ProjectManager</code> instance
   */
  @Nonnull
  public static ProjectManager getInstance() {
    return Application.get().getInstance(ProjectManager.class);
  }

  @Nonnull
  public abstract AsyncResult<Project> openProjectAsync(@Nonnull VirtualFile file, @Nonnull UIAccess uiAccess);

  @Nonnull
  public abstract AsyncResult<Project> openProjectAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess);

  @Nonnull
  public AsyncResult<Void> closeAndDisposeAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    return closeAndDisposeAsync(project, uiAccess, true, true, true);
  }

  @Nonnull
  public abstract AsyncResult<Void> closeAndDisposeAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess, boolean checkCanClose, boolean save, boolean dispose);

  /**
   * Adds listener to the specified project.
   *
   * @param project  project to add listener to
   * @param listener listener to add
   */
  public abstract void addProjectManagerListener(@Nonnull Project project, @Nonnull ProjectManagerListener listener);

  /**
   * Removes listener from the specified project.
   *
   * @param project  project to remove listener from
   * @param listener listener to remove
   */
  public abstract void removeProjectManagerListener(@Nonnull Project project, @Nonnull ProjectManagerListener listener);

  /**
   * Returns the list of currently opened projects.
   *
   * @return the array of currently opened projects.
   */
  @Nonnull
  public abstract Project[] getOpenProjects();

  /**
   * Returns the project which is used as a template for new projects. The template project
   * is always available, even when no other project is open.
   * NB: default project can be lazy loaded
   *
   * @return the template project instance.
   */
  @Nonnull
  public Project getDefaultProject() {
    return DefaultProjectFactory.getInstance().getDefaultProject();
  }

  /**
   * Closes the specified project.
   *
   * @param project the project to close.
   * @return true if the project was closed successfully, false if the closing was disallowed by the close listeners.
   */
  @RequiredUIAccess
  public abstract boolean closeProject(@Nonnull Project project);

  /**
   * Asynchronously reloads the specified project.
   *
   * @param project the project to reload.
   */
  public abstract void reloadProject(@Nonnull Project project, @Nonnull UIAccess uiAccess);

  /**
   * Create new project in given location.
   *
   * @param name project name
   * @param path project location
   *
   * @return newly crated project
   */
  @Nullable
  public abstract Project createProject(String name, String path);

  // region deprecated code
  /**
   * Adds global listener to all projects
   *
   * @param listener listener to add
   */
  @Deprecated
  @DeprecationInfo("Use ProjectManager#TOPIC")
  public abstract void addProjectManagerListener(@Nonnull ProjectManagerListener listener);

  @Deprecated
  @DeprecationInfo("Use ProjectManager#TOPIC")
  public abstract void addProjectManagerListener(@Nonnull ProjectManagerListener listener, @Nonnull Disposable parentDisposable);

  /**
   * Removes global listener from all projects.
   *
   * @param listener listener to remove
   */
  @Deprecated
  @DeprecationInfo("Use ProjectManager#TOPIC")
  public abstract void removeProjectManagerListener(@Nonnull ProjectManagerListener listener);
  
  //endregion
}
